package dev.schoenberg.evergore.protocolParser.database;

import static com.j256.ormlite.dao.DaoManager.*;
import static com.j256.ormlite.table.TableUtils.*;
import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.*;

import com.j256.ormlite.dao.*;
import com.j256.ormlite.jdbc.*;
import com.j256.ormlite.support.*;

import dev.schoenberg.evergore.protocolParser.helper.config.*;

public abstract class Repository {
	protected static <T> Dao<T, String> getDao(Configuration config, Class<T> type) {
		ConnectionSource con = silentThrow(() -> new JdbcConnectionSource("jdbc:sqlite:" + config.getDatabasePath()));

		if (config.initializeDatabase) {
			silentThrow(() -> dropTable(con, type, true));
			silentThrow(() -> createTable(con, type));
		}

		return silentThrow(() -> createDao(con, type));
	}
}
