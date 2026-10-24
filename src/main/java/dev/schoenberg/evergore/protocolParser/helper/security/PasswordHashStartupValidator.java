package dev.schoenberg.evergore.protocolParser.helper.security;

import jakarta.inject.Singleton;

import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.context.event.StartupEvent;

import dev.schoenberg.evergore.protocolParser.Logger;
import dev.schoenberg.evergore.protocolParser.helper.config.PasswordHashConfiguration;

@Singleton
public class PasswordHashStartupValidator implements ApplicationEventListener<StartupEvent> {
	private static final String MAX_CONCURRENT = "evergore.password-hash.max-concurrent";

	private final PasswordHashConfiguration configuration;
	private final Logger logger;

	public PasswordHashStartupValidator(PasswordHashConfiguration configuration, Logger logger) {
		this.configuration = configuration;
		this.logger = logger;
	}

	@Override
	public void onApplicationEvent(StartupEvent event) {
		validatePasswordHash();
	}

	void validatePasswordHash() {
		if (configuration.maxConcurrent() < 1) {
			String reason = "Configuration property '" + MAX_CONCURRENT + "' must be at least 1, otherwise no password could ever be checked.";
			logger.error(reason);
			throw new IllegalStateException(reason);
		}
	}
}
