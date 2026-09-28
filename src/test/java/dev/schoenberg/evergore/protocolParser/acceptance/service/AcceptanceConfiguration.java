package dev.schoenberg.evergore.protocolParser.acceptance.service;

import jakarta.inject.Singleton;

import io.micronaut.context.annotation.Replaces;
import io.micronaut.context.annotation.Requires;

import dev.schoenberg.evergore.protocolParser.acceptance.world.ScenarioDatabase;
import dev.schoenberg.evergore.protocolParser.helper.config.Configuration;

@Singleton
@Replaces(Configuration.class)
@Requires(env = AcceptanceEnvironment.NAME)
class AcceptanceConfiguration extends Configuration {
	private final ScenarioDatabase database;

	AcceptanceConfiguration(ScenarioDatabase database) {
		this.database = database;
	}

	@Override
	public String getDatabasePath() {
		return database.file().toString();
	}

	@Override
	public int getCollectorInitialDelaySeconds() {
		return 0;
	}
}
