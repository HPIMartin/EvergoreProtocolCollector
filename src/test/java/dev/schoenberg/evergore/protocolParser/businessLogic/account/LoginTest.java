package dev.schoenberg.evergore.protocolParser.businessLogic.account;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LoginTest {
	private static final Instant ISSUED_AT = Instant.parse("2026-01-05T10:00:00Z");

	@Test
	void isTheLoginOfTheAccountItNamesInAnyCapitals() {
		Login tested = login("AURORA", LoginSource.ACCOUNT);

		boolean theLoginOfIt = tested.isTheLoginOf(account("Aurora"));

		assertThat(theLoginOfIt).isTrue();
	}

	@Test
	void isNotTheLoginOfAnotherAccount() {
		Login tested = login("Aurora", LoginSource.ACCOUNT);

		boolean theLoginOfIt = tested.isTheLoginOf(account("Boreas"));

		assertThat(theLoginOfIt).isFalse();
	}

	@Test
	void isNoAccountsLoginWhenItCameFromTheAdminKey() {
		Login tested = login("admin", LoginSource.ADMIN_KEY);

		boolean theLoginOfIt = tested.isTheLoginOf(account("admin"));

		assertThat(theLoginOfIt).isFalse();
	}

	private static Login login(String accountName, LoginSource source) {
		return new Login(accountName, Role.ADMIN, 0, source, "id", ISSUED_AT, ISSUED_AT.plus(Login.LIFETIME));
	}

	private static Account account(String avatar) {
		return new Account(avatar, "hash", PasswordState.CHOSEN, Role.ADMIN, Optional.empty());
	}
}
