package dev.schoenberg.evergore.protocolParser;

import jakarta.inject.Singleton;

import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.annotation.Value;

import dev.schoenberg.evergore.protocolParser.helper.config.Configuration;

@Factory
@Requires(property = ThrowawayDatabaseFactory.THROWAWAY_DATABASE_PATH)
public class ThrowawayDatabaseFactory {
	public static final String THROWAWAY_DATABASE_PATH = "test.throwaway-database-path";

	private static final int NO_SCRAPE_WHILE_THE_SUITE_RUNS = Integer.MAX_VALUE;

	@Singleton
	@Replaces(Configuration.class)
	Configuration throwawayDatabaseConfiguration(@Value("${" + THROWAWAY_DATABASE_PATH + "}") String databasePath) {
		return new Configuration() {
			@Override
			public String getDatabasePath() {
				return databasePath;
			}

			@Override
			public int getCollectorInitialDelaySeconds() {
				return NO_SCRAPE_WHILE_THE_SUITE_RUNS;
			}
		};
	}
}
