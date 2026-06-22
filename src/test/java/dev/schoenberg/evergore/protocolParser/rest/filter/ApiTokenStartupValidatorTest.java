package dev.schoenberg.evergore.protocolParser.rest.filter;

import org.junit.jupiter.api.Test;

import dev.schoenberg.evergore.protocolParser.LoggerSpy;
import dev.schoenberg.evergore.protocolParser.helper.config.SecurityConfiguration;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApiTokenStartupValidatorTest {

	@Test
	void emptyTokenCausesStartupFailureWithPropertyNameInMessage() {
		assertStartupFailsWithPropertyName("");
	}

	@Test
	void blankTokenCausesStartupFailureWithPropertyNameInMessage() {
		assertStartupFailsWithPropertyName("   ");
	}

	@Test
	void nullTokenCausesStartupFailureWithPropertyNameInMessage() {
		assertStartupFailsWithPropertyName(null);
	}

	@Test
	void setTokenAllowsStartup() {
		ApiTokenStartupValidator validator = validatorWithToken("some-valid-token");

		assertThatNoException().isThrownBy(validator::validateApiToken);
	}

	@Test
	void onApplicationEventPropagatesFailureWhenTokenIsBlank() {
		ApiTokenStartupValidator validator = validatorWithToken("");

		assertThatThrownBy(() -> validator.onApplicationEvent(null)).isInstanceOf(IllegalStateException.class).hasMessageContaining("evergore.security.api-token");
	}

	private void assertStartupFailsWithPropertyName(String token) {
		ApiTokenStartupValidator validator = validatorWithToken(token);

		assertThatThrownBy(validator::validateApiToken).isInstanceOf(IllegalStateException.class).hasMessageContaining("evergore.security.api-token");
	}

	private ApiTokenStartupValidator validatorWithToken(String token) {
		return new ApiTokenStartupValidator(new SecurityConfiguration(token), new LoggerSpy());
	}
}
