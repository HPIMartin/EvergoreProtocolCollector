package dev.schoenberg.evergore.protocolParser.helper.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import dev.schoenberg.evergore.protocolParser.LoggerSpy;
import dev.schoenberg.evergore.protocolParser.helper.config.PasswordHashConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordHashStartupValidatorTest {
	private static final String PROPERTY = "evergore.password-hash.max-concurrent";

	@ParameterizedTest
	@ValueSource(ints = {1, 2, 8})
	void aCapOfAtLeastOneCheckAllowsStartup(int maxConcurrent) {
		PasswordHashStartupValidator tested = validatorWith(maxConcurrent, new LoggerSpy());

		assertThatNoException().isThrownBy(tested::validatePasswordHash);
	}

	@ParameterizedTest
	@ValueSource(ints = {0, -1})
	void aCapOfNoCheckCausesStartupFailureNamingTheProperty(int maxConcurrent) {
		PasswordHashStartupValidator tested = validatorWith(maxConcurrent, new LoggerSpy());

		assertThatThrownBy(tested::validatePasswordHash).isInstanceOf(IllegalStateException.class).hasMessageContaining(PROPERTY);
	}

	@Test
	void logsTheRefusedCapAsAnError() {
		LoggerSpy logger = new LoggerSpy();
		PasswordHashStartupValidator tested = validatorWith(0, logger);

		assertThatThrownBy(tested::validatePasswordHash).isInstanceOf(IllegalStateException.class);

		assertThat(logger.errorMessages()).anyMatch(message -> message.contains(PROPERTY));
	}

	private static PasswordHashStartupValidator validatorWith(int maxConcurrent, LoggerSpy logger) {
		return new PasswordHashStartupValidator(new PasswordHashConfiguration(1024, 1, 1, maxConcurrent), logger);
	}
}
