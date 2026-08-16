package dev.schoenberg.evergore.protocolParser.dataExtraction.website;

import java.util.Map;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import dev.schoenberg.evergore.protocolParser.LoggerSpy;
import dev.schoenberg.evergore.protocolParser.helper.config.CredentialsConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

class CredentialsStartupValidatorTest {
	private final LoggerSpy logger = new LoggerSpy();

	@Test
	void nullUsernameCausesStartupFailureWithPropertyNameInMessage() {
		assertStartupFailsNaming(null, "password", "evergore.credentials.username");
	}

	@Test
	void emptyUsernameCausesStartupFailureWithPropertyNameInMessage() {
		assertStartupFailsNaming("", "password", "evergore.credentials.username");
	}

	@Test
	void blankUsernameCausesStartupFailureWithPropertyNameInMessage() {
		assertStartupFailsNaming("   ", "password", "evergore.credentials.username");
	}

	@Test
	void nullPasswordCausesStartupFailureWithPropertyNameInMessage() {
		assertStartupFailsNaming("username", null, "evergore.credentials.password");
	}

	@Test
	void emptyPasswordCausesStartupFailureWithPropertyNameInMessage() {
		assertStartupFailsNaming("username", "", "evergore.credentials.password");
	}

	@Test
	void blankPasswordCausesStartupFailureWithPropertyNameInMessage() {
		assertStartupFailsNaming("username", "   ", "evergore.credentials.password");
	}

	@Test
	void setCredentialsAllowStartup() {
		CredentialsStartupValidator tested = validatorFor("username", "password");

		assertThatNoException().isThrownBy(tested::validateCredentials);
	}

	@Test
	void onApplicationEventPropagatesFailureWhenUsernameIsBlank() {
		CredentialsStartupValidator tested = validatorFor("", "password");

		assertThatThrownBy(() -> tested.onApplicationEvent(null)).isInstanceOf(IllegalStateException.class).hasMessageContaining("evergore.credentials.username");
	}

	@Test
	void failureIsLoggedWithTheEnvironmentVariableThatSetsIt() {
		CredentialsStartupValidator tested = validatorFor("username", "");

		assertThatThrownBy(tested::validateCredentials).isInstanceOf(IllegalStateException.class);
		assertThat(logger.errorMessages()).singleElement().asString().contains("EVERGORE_CREDENTIALS_PASSWORD");
	}

	@Test
	void neitherCredentialIsEverLogged() {
		CredentialsStartupValidator tested = validatorFor("the-username", "");

		assertThatThrownBy(tested::validateCredentials).isInstanceOf(IllegalStateException.class);
		assertThat(logger.errorMessages()).noneMatch(message -> message.contains("the-username"));
	}

	@Test
	void aBlankLoginStopsTheApplicationContextFromStarting() {
		Map<String, Object> blankLogin = Map.of("evergore.credentials.username", "", "evergore.credentials.password", "", "micronaut.server.port", "-1");

		Throwable thrown = catchThrowable(() -> ApplicationContext.run(blankLogin, "test").close());

		assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessage("Required configuration property 'evergore.credentials.username' is not set or blank.");
	}

	private void assertStartupFailsNaming(String username, String password, String property) {
		CredentialsStartupValidator tested = validatorFor(username, password);

		assertThatThrownBy(tested::validateCredentials).isInstanceOf(IllegalStateException.class).hasMessageContaining(property);
	}

	private CredentialsStartupValidator validatorFor(String username, String password) {
		return new CredentialsStartupValidator(new CredentialsConfiguration(username, password), logger);
	}
}
