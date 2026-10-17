package dev.schoenberg.evergore.protocolParser.database;

import java.nio.file.Path;
import java.sql.SQLException;
import java.util.concurrent.Callable;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.support.DatabaseConnection;

import dev.schoenberg.evergore.protocolParser.Logger;
import dev.schoenberg.evergore.protocolParser.helper.config.Configuration;

import static com.j256.ormlite.dao.DaoManager.createDao;
import static com.j256.ormlite.support.DatabaseConnection.DEFAULT_RESULT_FLAGS;
import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.silentThrow;
import static java.nio.file.Files.createDirectories;

public class SqliteDatabase implements AutoCloseable {
	private final ConnectionSource connections;

	SqliteDatabase(ConnectionSource connections) {
		this.connections = connections;
	}

	public static SqliteDatabase open(Configuration config, PreDatabaseConnectionHook hook, Logger logger) {
		return silentThrow(() -> {
			String dbPath = config.getDatabasePath();
			Path parent = Path.of(dbPath).getParent();
			if (parent != null) {
				createDirectories(parent);
			}
			hook.run();
			new DatabaseMigration(config, logger).migrate();
			String url = "jdbc:sqlite:" + dbPath;
			logger.info("Connecting to: " + url);
			return new SqliteDatabase(new GermanOrderConnectionSource(url + "?busy_timeout=" + config.getDatabaseBusyTimeout().toMillis()));
		});
	}

	public <T> Dao<T, String> dao(Class<T> type) {
		return silentThrow(() -> createDao(connections, type));
	}

	public <R> R inTransaction(Callable<R> work) {
		return silentThrow(() -> {
			DatabaseConnection connection = connections.getReadWriteConnection(null);
			connections.saveSpecialConnection(connection);
			try {
				return committed(connection, work);
			} finally {
				connections.clearSpecialConnection(connection);
				connections.releaseConnection(connection);
			}
		});
	}

	@Override
	public void close() {
		silentThrow(() -> connections.close());
	}

	private static <R> R committed(DatabaseConnection connection, Callable<R> work) throws Exception {
		connection.executeStatement("BEGIN", DEFAULT_RESULT_FLAGS);
		try {
			R result = work.call();
			connection.executeStatement("COMMIT", DEFAULT_RESULT_FLAGS);
			return result;
		} catch (Throwable failure) {
			rollBack(connection, failure);
			throw failure;
		}
	}

	private static void rollBack(DatabaseConnection connection, Throwable failure) {
		try {
			connection.executeStatement("ROLLBACK", DEFAULT_RESULT_FLAGS);
		} catch (SQLException rollbackFailure) {
			failure.addSuppressed(rollbackFailure);
		}
	}
}
