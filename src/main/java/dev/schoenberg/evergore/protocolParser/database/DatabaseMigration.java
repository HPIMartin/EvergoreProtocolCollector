package dev.schoenberg.evergore.protocolParser.database;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;

import dev.schoenberg.evergore.protocolParser.Logger;
import dev.schoenberg.evergore.protocolParser.helper.config.Configuration;

public class DatabaseMigration {
	private static final String MIGRATIONS = "classpath:db/migration";
	private static final String BEFORE_THE_FIRST_MIGRATION = "0";
	private static final Object ONE_MIGRATION_AT_A_TIME = new Object();

	private final Configuration config;
	private final Logger logger;

	public DatabaseMigration(Configuration config, Logger logger) {
		this.config = config;
		this.logger = logger;
	}

	public void migrate() {
		synchronized (ONE_MIGRATION_AT_A_TIME) {
			MigrateResult result = Flyway
					.configure()
					.dataSource("jdbc:sqlite:" + config.getDatabasePath(), null, null)
					.locations(MIGRATIONS)
					.baselineOnMigrate(true)
					.baselineVersion(BEFORE_THE_FIRST_MIGRATION)
					.load()
					.migrate();

			if (result.migrationsExecuted == 0) {
				logger.info("Database schema already at version " + result.initialSchemaVersion);
			} else {
				logger.info("Database schema migrated to version " + result.targetSchemaVersion + " by " + result.migrationsExecuted + " migration(s)");
			}
		}
	}
}
