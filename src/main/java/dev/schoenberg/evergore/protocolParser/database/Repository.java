package dev.schoenberg.evergore.protocolParser.database;

import java.nio.file.Path;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;

import dev.schoenberg.evergore.protocolParser.Logger;
import dev.schoenberg.evergore.protocolParser.helper.config.Configuration;

import static com.j256.ormlite.dao.DaoManager.createDao;
import static com.j256.ormlite.table.TableUtils.createTableIfNotExists;
import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.silentThrow;
import static java.nio.file.Files.createDirectories;

public abstract class Repository<T> {
	private final ConnectionSource con;
	private final Class<T> type;
	protected final Logger logger;

	public Repository(ConnectionSource con, Logger logger, Class<T> type) {
		this.con = con;
		this.logger = logger;
		this.type = type;
	}

	protected static ConnectionSource getCon(Configuration config, Logger logger, PreDatabaseConnectionHook hook) {
		return silentThrow(() -> {
			hook.run();
			String dbPath = config.getDatabasePath();
			Path parent = Path.of(dbPath).getParent();
			if (parent != null) {
				createDirectories(parent);
			}
			String url = "jdbc:sqlite:" + dbPath;
			logger.info("Connecting to: " + url);
			return new JdbcConnectionSource(url);
		});
	}

	protected static <T> Dao<T, String> getDao(ConnectionSource con, Class<T> type) {
		return silentThrow(() -> createDao(con, type));
	}

	protected void ensureTable() {
		silentThrow(() -> {
			if (createTableIfNotExists(con, type) != 0) {
				logger.info("Created table for " + type.getSimpleName());
			} else {
				logger.info("Table already exists for: " + type.getSimpleName());
			}
		});
	}
}
