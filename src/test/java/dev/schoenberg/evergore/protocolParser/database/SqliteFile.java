package dev.schoenberg.evergore.protocolParser.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.silentThrow;

public class SqliteFile {
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
}
