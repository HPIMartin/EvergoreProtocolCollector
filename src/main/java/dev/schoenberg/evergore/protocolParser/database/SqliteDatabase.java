package dev.schoenberg.evergore.protocolParser.database;

import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.Callable;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.jdbc.JdbcPooledConnectionSource;
import com.j256.ormlite.misc.TransactionManager;
import com.j256.ormlite.support.ConnectionSource;

import dev.schoenberg.evergore.protocolParser.Logger;
import dev.schoenberg.evergore.protocolParser.helper.config.Configuration;

import static com.j256.ormlite.dao.DaoManager.createDao;
import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.silentThrow;
import static java.nio.file.Files.createDirectories;

public class SqliteDatabase implements AutoCloseable {
	private static final Duration BUSY_TIMEOUT = Duration.ofSeconds(10);

	private final ConnectionSource connections;

	private SqliteDatabase(ConnectionSource connections) {
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
			return new SqliteDatabase(new JdbcPooledConnectionSource(url + "?busy_timeout=" + BUSY_TIMEOUT.toMillis()));
		});
	}

	public <T> Dao<T, String> dao(Class<T> type) {
		return silentThrow(() -> createDao(connections, type));
	}

	public <R> R inTransaction(Callable<R> work) {
		return silentThrow(() -> TransactionManager.callInTransaction(connections, work));
	}

	@Override
	public void close() {
		silentThrow(() -> connections.close());
	}
}
