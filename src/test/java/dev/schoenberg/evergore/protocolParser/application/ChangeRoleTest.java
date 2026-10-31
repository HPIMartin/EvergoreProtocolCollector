package dev.schoenberg.evergore.protocolParser.application;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import dev.schoenberg.evergore.protocolParser.businessLogic.account.Account;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.Login;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.LoginSource;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.PasswordState;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.Role;

import static org.assertj.core.api.Assertions.assertThat;

class ChangeRoleTest {
	private static final Instant LOGGED_IN = Instant.parse("2026-01-05T10:00:00Z");
	private static final Login THE_ADMIN_KEYS_LOGIN = loginOf(AdminKey.NAME, LoginSource.ADMIN_KEY);

	private final InMemoryAccounts accounts = new InMemoryAccounts(new ArrayList<>());
	private final ChangeRole tested = new ChangeRole(accounts);

	@Test
	void givesTheAccountTheRoleItWasAskedFor() {
		accounts.hold(account("Aurora", Role.MEMBER));

		RoleChangeOutcome outcome = tested.to("Aurora", Role.ADMIN, THE_ADMIN_KEYS_LOGIN);

		assertThat(accounts.held("aurora")).isEqualTo(account("Aurora", Role.ADMIN));
		assertThat(outcome).isInstanceOf(RoleChangeOutcome.Changed.class);
	}

	private static Login loginOf(String accountName, LoginSource source) {
		return new Login(accountName, Role.ADMIN, 0, source, "id", LOGGED_IN, LOGGED_IN.plus(Login.LIFETIME));
	}

	private static Account account(String avatar, Role role) {
		return new Account(avatar, "hash", PasswordState.CHOSEN, role, Optional.empty());
	}
}
