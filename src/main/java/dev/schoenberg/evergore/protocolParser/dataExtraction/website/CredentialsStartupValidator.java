package dev.schoenberg.evergore.protocolParser.dataExtraction.website;

import jakarta.inject.Singleton;

import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.context.event.StartupEvent;

import dev.schoenberg.evergore.protocolParser.Logger;
import dev.schoenberg.evergore.protocolParser.helper.config.CredentialsConfiguration;

@Singleton
public class CredentialsStartupValidator implements ApplicationEventListener<StartupEvent> {
	private static final String USERNAME_PROPERTY = "evergore.credentials.username";
	private static final String PASSWORD_PROPERTY = "evergore.credentials.password";
	private static final String USERNAME_VARIABLE = "EVERGORE_CREDENTIALS_USERNAME";
	private static final String PASSWORD_VARIABLE = "EVERGORE_CREDENTIALS_PASSWORD";

	private final CredentialsConfiguration credentials;
	private final Logger logger;

	public CredentialsStartupValidator(CredentialsConfiguration credentials, Logger logger) {
		this.credentials = credentials;
		this.logger = logger;
	}

	@Override
	public void onApplicationEvent(StartupEvent event) {
		validateCredentials();
	}

	void validateCredentials() {
		requireSet(credentials.username(), USERNAME_PROPERTY, USERNAME_VARIABLE);
		requireSet(credentials.password(), PASSWORD_PROPERTY, PASSWORD_VARIABLE);
	}

	private void requireSet(String configured, String property, String variable) {
		if (configured == null || configured.isBlank()) {
			String reason = "Required configuration property '" + property + "' is not set or blank.";
			logger.error(reason + " Set the environment variable " + variable + ".");
			throw new IllegalStateException(reason);
		}
	}
}
