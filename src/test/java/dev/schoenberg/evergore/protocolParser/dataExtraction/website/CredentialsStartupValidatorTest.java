package dev.schoenberg.evergore.protocolParser.dataExtraction.website;

import java.util.Map;
import java.util.Optional;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import dev.schoenberg.evergore.protocolParser.LoggerSpy;
import dev.schoenberg.evergore.protocolParser.helper.config.CredentialsConfiguration;

import static dev.schoenberg.evergore.protocolParser.ThrowawayDatabaseFactory.THROWAWAY_DATABASE_PATH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

class CredentialsStartupValidatorTest {
	private static final String THROWAWAY_DB_PATH = "build/tmp/test/credentialsStartupValidatorTest.sqlite";

	private final LoggerSpy logger = new LoggerSpy();

	@Test
	void absentUsernameCausesStartupFailureWithPropertyNameInMessage() {
		assertStartupFailsNaming(Optional.empty(), Optional.of("password"), "evergore.credentials.username");
	}

	@Test
	void emptyUsernameCausesStartupFailureWithPropertyNameInMessage() {
		assertStartupFailsNaming(Optional.of(""), Optional.of("password"), "evergore.credentials.username");
	}

	@Test
	void blankUsernameCausesStartupFailureWithPropertyNameInMessage() {
		assertStartupFailsNaming(Optional.of("   "), Optional.of("password"), "evergore.credentials.username");
	}

	@Test
	void absentPasswordCausesStartupFailureWithPropertyNameInMessage() {
		assertStartupFailsNaming(Optional.of("username"), Optional.empty(), "evergore.credentials.password");
	}

	@Test
	void emptyPasswordCausesStartupFailureWithPropertyNameInMessage() {
		assertStartupFailsNaming(Optional.of("username"), Optional.of(""), "evergore.credentials.password");
	}

	@Test
	void blankPasswordCausesStartupFailureWithPropertyNameInMessage() {
		assertStartupFailsNaming(Optional.of("username"), Optional.of("   "), "evergore.credentials.password");
	}

	@Test
	void setCredentialsAllowStartup() {
		CredentialsStartupValidator tested = validatorFor(Optional.of("username"), Optional.of("password"));

		assertThatNoException().isThrownBy(tested::validateCredentials);
	}

	@Test
	void onApplicationEventPropagatesFailureWhenUsernameIsBlank() {
		CredentialsStartupValidator tested = validatorFor(Optional.of(""), Optional.of("password"));

		assertThatThrownBy(() -> tested.onApplicationEvent(null)).isInstanceOf(IllegalStateException.class).hasMessageContaining("evergore.credentials.username");
	}

	@Test
	void failureIsLoggedWithTheEnvironmentVariableThatSetsIt() {
		CredentialsStartupValidator tested = validatorFor(Optional.of("username"), Optional.of(""));

		assertThatThrownBy(tested::validateCredentials).isInstanceOf(IllegalStateException.class);
		assertThat(logger.errorMessages()).singleElement().asString().contains("EVERGORE_CREDENTIALS_PASSWORD");
	}

	@Test
	void neitherCredentialIsEverLogged() {
		CredentialsStartupValidator tested = validatorFor(Optional.of("the-username"), Optional.of(""));

		assertThatThrownBy(tested::validateCredentials).isInstanceOf(IllegalStateException.class);
		assertThat(logger.errorMessages()).noneMatch(message -> message.contains("the-username"));
	}

	@Test
	void aBlankLoginStopsTheApplicationContextFromStarting() {
		Map<String, Object> blankLogin = Map
				.of("evergore.credentials.username", "", "evergore.credentials.password", "", "micronaut.server.port", "-1", THROWAWAY_DATABASE_PATH, THROWAWAY_DB_PATH);

		Throwable thrown = catchThrowable(() -> ApplicationContext.run(blankLogin, "test").close());

		assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessage("Required configuration property 'evergore.credentials.username' is not set or blank.");
	}

	@Test
	void anUnsetUsernameStopsTheApplicationContextFromStarting() {
		Map<String, Object> unsetUsername = Map
				.of("evergore.security.api-token", "token", "evergore.credentials.password", "secret", "micronaut.server.port", "-1", THROWAWAY_DATABASE_PATH, THROWAWAY_DB_PATH);

		Throwable thrown = catchThrowable(() -> ApplicationContext.builder().deduceEnvironment(false).environmentPropertySource(false).properties(unsetUsername).start().close());

		assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessage("Required configuration property 'evergore.credentials.username' is not set or blank.");
	}

	@Test
	void anUnsetPasswordStopsTheApplicationContextFromStarting() {
		Map<String, Object> unsetPassword = Map
				.of("evergore.security.api-token", "token", "evergore.credentials.username", "user", "micronaut.server.port", "-1", THROWAWAY_DATABASE_PATH, THROWAWAY_DB_PATH);

		Throwable thrown = catchThrowable(() -> ApplicationContext.builder().deduceEnvironment(false).environmentPropertySource(false).properties(unsetPassword).start().close());

		assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessage("Required configuration property 'evergore.credentials.password' is not set or blank.");
	}

	private void assertStartupFailsNaming(Optional<String> username, Optional<String> password, String property) {
		CredentialsStartupValidator tested = validatorFor(username, password);

		assertThatThrownBy(tested::validateCredentials).isInstanceOf(IllegalStateException.class).hasMessageContaining(property);
	}

	private CredentialsStartupValidator validatorFor(Optional<String> username, Optional<String> password) {
		return new CredentialsStartupValidator(new CredentialsConfiguration(username, password), logger);
	}
}
