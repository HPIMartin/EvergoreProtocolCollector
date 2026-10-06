package dev.schoenberg.evergore.protocolParser.database;

import java.io.RandomAccessFile;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.silentThrow;

public class SqliteFile {
	private static final int CHANGE_COUNTER_OFFSET = 24;

	private final String path;

	public SqliteFile(String path) {
		this.path = path;
	}

	public void execute(String sql) {
		silentThrow(() -> {
			try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + path); Statement statement = connection.createStatement()) {
				statement.execute(sql);
			}
		});
	}

	public void takeAndReleaseTheExclusiveLockWithoutWaiting() {
		silentThrow(() -> {
			try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + path + "?busy_timeout=0"); Statement statement = connection.createStatement()) {
				statement.execute("BEGIN EXCLUSIVE");
				statement.execute("ROLLBACK");
			}
		});
	}

	public List<Integer> committedValues(String table, String column) {
		return committedValues(table, column, column);
	}

	public List<Integer> committedValues(String table, String column, String orderColumn) {
		return silentThrow(() -> {
			try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + path);
					Statement statement = connection.createStatement();
					ResultSet rows = statement.executeQuery("SELECT " + column + " FROM " + table + " ORDER BY " + orderColumn)) {
				List<Integer> values = new ArrayList<>();
				while (rows.next()) {
					values.add(rows.getInt(1));
				}
				return values;
			}
		});
	}

	public int commitsDuring(Runnable work) {
		int before = changeCounter();
		work.run();
		return changeCounter() - before;
	}

	private int changeCounter() {
		return silentThrow(() -> {
			try (RandomAccessFile file = new RandomAccessFile(path, "r")) {
				file.seek(CHANGE_COUNTER_OFFSET);
				return file.readInt();
			}
		});
	}
}
