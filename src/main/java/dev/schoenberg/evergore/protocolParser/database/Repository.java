package dev.schoenberg.evergore.protocolParser.database;

import java.nio.file.Path;
import java.util.concurrent.Callable;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.misc.TransactionManager;
import com.j256.ormlite.support.ConnectionSource;

import dev.schoenberg.evergore.protocolParser.Logger;
import dev.schoenberg.evergore.protocolParser.helper.config.Configuration;

import static com.j256.ormlite.dao.DaoManager.createDao;
import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.silentThrow;
import static java.nio.file.Files.createDirectories;

public abstract class Repository<T> {
	private final ConnectionSource con;
	protected final Logger logger;

	public Repository(ConnectionSource con, Logger logger) {
		this.con = con;
		this.logger = logger;
	}

	protected static ConnectionSource getCon(Configuration config, Logger logger, PreDatabaseConnectionHook hook) {
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
			return new JdbcConnectionSource(url);
		});
	}

	protected static <T> Dao<T, String> getDao(ConnectionSource con, Class<T> type) {
		return silentThrow(() -> createDao(con, type));
	}

	protected <R> R inTransaction(Callable<R> work) {
		return silentThrow(() -> TransactionManager.callInTransaction(con, work));
	}

}
