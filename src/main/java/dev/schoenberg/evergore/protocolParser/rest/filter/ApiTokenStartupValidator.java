package dev.schoenberg.evergore.protocolParser.rest.filter;

import jakarta.inject.Singleton;

import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.context.event.StartupEvent;

import dev.schoenberg.evergore.protocolParser.Logger;
import dev.schoenberg.evergore.protocolParser.helper.config.SecurityConfiguration;

@Singleton
public class ApiTokenStartupValidator implements ApplicationEventListener<StartupEvent> {

	private final SecurityConfiguration securityConfiguration;
	private final Logger logger;

	public ApiTokenStartupValidator(SecurityConfiguration securityConfiguration, Logger logger) {
		this.securityConfiguration = securityConfiguration;
		this.logger = logger;
	}

	@Override
	public void onApplicationEvent(StartupEvent event) {
		validateApiToken();
	}

	void validateApiToken() {
		String token = securityConfiguration.apiToken();
		if (token == null || token.isBlank()) {
			logger.error("Required configuration property 'evergore.security.api-token' is not set or blank. Set the environment variable EVERGORE_SECURITY_API_TOKEN.");
			throw new IllegalStateException("Required configuration property 'evergore.security.api-token' is not set or blank.");
		}
	}
}
