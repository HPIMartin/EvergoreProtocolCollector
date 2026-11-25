package dev.schoenberg.evergore.protocolParser.application;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import dev.schoenberg.evergore.protocolParser.LoggerSpy;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.Account;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.Login;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.LoginSource;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.PasswordState;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.Role;

import static org.assertj.core.api.Assertions.assertThat;

class ChangeRoleOrderTest {
	private static final Instant LOGGED_IN = Instant.parse("2026-01-05T10:00:00Z");
	private static final Login THE_ADMIN_KEYS_LOGIN = new Login(AdminKey.NAME, Role.ADMIN, 0, LoginSource.ADMIN_KEY, "id", LOGGED_IN, LOGGED_IN.plus(Login.LIFETIME));

	private final LoginGenerations generations = new LoginGenerations();
	private final List<Long> generationsAtTheWrite = new ArrayList<>();
	private final InMemoryAccounts accounts = new InMemoryAccounts(new ArrayList<>()) {
		@Override
		public boolean replaceRole(String nameKey, Role role) {
			generationsAtTheWrite.add(generations.of(nameKey));
			return super.replaceRole(nameKey, role);
		}
	};
	private final ChangeRole tested = new ChangeRole(accounts, generations, new LoggerSpy());

	@Test
	void storesTheNewRoleBeforeItEndsTheLogins() {
		accounts.hold(new Account("Boreas", "hash", PasswordState.CHOSEN, Role.ADMIN, Optional.empty()));

		tested.to("Boreas", Role.MEMBER, THE_ADMIN_KEYS_LOGIN);

		long generationAfterTheChange = generations.of("boreas");
		assertThat(generationsAtTheWrite).containsExactly(generationAfterTheChange - 1);
	}
}
