package dev.schoenberg.evergore.protocolParser.rest.filter;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import dev.schoenberg.evergore.protocolParser.LoggerSpy;
import dev.schoenberg.evergore.protocolParser.helper.config.SecurityConfiguration;

import static dev.schoenberg.evergore.protocolParser.ThrowawayDatabaseFactory.THROWAWAY_DATABASE_PATH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

class ApiTokenStartupValidatorTest {
	private static final String THROWAWAY_DB_PATH = "build/tmp/test/apiTokenStartupValidatorTest.sqlite";

	private final LoggerSpy logger = new LoggerSpy();

	@Test
	void emptyTokenCausesStartupFailureWithPropertyNameInMessage() {
		assertStartupFailsWithPropertyName(Optional.of(""));
	}

	@Test
	void blankTokenCausesStartupFailureWithPropertyNameInMessage() {
		assertStartupFailsWithPropertyName(Optional.of("   "));
	}

	@Test
	void absentTokenCausesStartupFailureWithPropertyNameInMessage() {
		assertStartupFailsWithPropertyName(Optional.empty());
	}

	@Test
	void setTokenAllowsStartup() {
		ApiTokenStartupValidator validator = validatorWithToken(Optional.of("some-valid-token"));

		assertThatNoException().isThrownBy(validator::validateApiToken);
	}

	@Test
	void onApplicationEventPropagatesFailureWhenTokenIsBlank() {
		ApiTokenStartupValidator validator = validatorWithToken(Optional.of(""));

		assertThatThrownBy(() -> validator.onApplicationEvent(null)).isInstanceOf(IllegalStateException.class).hasMessageContaining("evergore.security.api-token");
	}

	@Test
	void anAbsentTokenIsLoggedWithTheEnvironmentVariableThatSetsIt() {
		ApiTokenStartupValidator tested = validatorWithToken(Optional.empty());

		Throwable thrown = catchThrowable(tested::validateApiToken);

		assertThat(thrown).isInstanceOf(IllegalStateException.class);
		assertThat(logger.errorMessages()).singleElement().asString().contains("EVERGORE_SECURITY_API_TOKEN");
	}

	@Test
	void anUnsetTokenStopsTheApplicationContextFromStarting() {
		Map<String, Object> unsetToken = Map
				.of("evergore.credentials.username", "user", "evergore.credentials.password", "secret", "micronaut.server.port", "-1", THROWAWAY_DATABASE_PATH, THROWAWAY_DB_PATH);

		Throwable thrown = catchThrowable(() -> ApplicationContext.builder().deduceEnvironment(false).environmentPropertySource(false).properties(unsetToken).start().close());

		assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessage("Required configuration property 'evergore.security.api-token' is not set or blank.");
	}

	private void assertStartupFailsWithPropertyName(Optional<String> token) {
		ApiTokenStartupValidator validator = validatorWithToken(token);

		assertThatThrownBy(validator::validateApiToken).isInstanceOf(IllegalStateException.class).hasMessageContaining("evergore.security.api-token");
	}

	private ApiTokenStartupValidator validatorWithToken(Optional<String> token) {
		return new ApiTokenStartupValidator(new SecurityConfiguration(token, List.of()), logger);
	}
}
