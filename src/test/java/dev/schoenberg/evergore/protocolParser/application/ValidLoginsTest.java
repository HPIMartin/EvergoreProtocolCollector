package dev.schoenberg.evergore.protocolParser.application;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import dev.schoenberg.evergore.protocolParser.businessLogic.account.Login;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.LoginSource;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.Role;

import static org.assertj.core.api.Assertions.assertThat;

class ValidLoginsTest {
	private static final Instant MADE = Instant.parse("2026-01-05T10:00:00Z");
	private static final String VALUE = "signed-login-value";

	private final RecordingLogins signed = new RecordingLogins();
	private final LoginGenerations generations = new LoginGenerations();
	private final ValidLogins tested = new ValidLogins(signed, generations);

	@Test
	void answersTheLoginOfAnAccountWhoseGenerationHasNotMovedOn() {
		Login login = accountLogin("Aurora", 0);
		signed.readAs(VALUE, login);

		Optional<Login> valid = tested.behind(VALUE);

		assertThat(valid).contains(login);
	}

	@Test
	void answersNothingForAValueTheServiceDidNotSign() {
		Optional<Login> valid = tested.behind("not-signed-by-the-service");

		assertThat(valid).isEmpty();
	}

	@Test
	void answersNothingForALoginOfAnEarlierGenerationOfItsAccount() {
		signed.readAs(VALUE, accountLogin("Aurora", 0));
		generations.bump("aurora");

		Optional<Login> valid = tested.behind(VALUE);

		assertThat(valid).isEmpty();
	}

	@Test
	void keepsTheLoginOfAnAccountWhenAnotherAccountsGenerationMovedOn() {
		Login login = accountLogin("Aurora", 0);
		signed.readAs(VALUE, login);
		generations.bump("boreas");

		Optional<Login> valid = tested.behind(VALUE);

		assertThat(valid).contains(login);
	}

	@Test
	void takesTheAccountNameInAnyCapitalsForTheSameAccount() {
		signed.readAs(VALUE, accountLogin("AURORA", 0));
		generations.bump("aurora");

		Optional<Login> valid = tested.behind(VALUE);

		assertThat(valid).isEmpty();
	}

	@Test
	void answersTheAdminKeyLoginWhateverTheGenerationOfAnAccountNamedLikeIt() {
		Login login = new Login("admin", Role.ADMIN, 0, LoginSource.ADMIN_KEY, "id", MADE, MADE.plus(Login.LIFETIME));
		signed.readAs(VALUE, login);
		generations.bump("admin");

		Optional<Login> valid = tested.behind(VALUE);

		assertThat(valid).contains(login);
	}

	private static Login accountLogin(String accountName, long generation) {
		return new Login(accountName, Role.MEMBER, generation, LoginSource.ACCOUNT, "id", MADE, MADE.plus(Login.LIFETIME));
	}
}
