package dev.schoenberg.evergore.protocolParser.application;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import dev.schoenberg.evergore.protocolParser.LoggerSpy;
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
	private final LoginGenerations generations = new LoginGenerations();
	private final LoggerSpy logger = new LoggerSpy();
	private final ChangeRole tested = new ChangeRole(accounts, generations, logger);

	static Stream<Arguments> changesWithTheLineTheyLog() {
		return Stream
				.of(Arguments.of("Aurora", Role.MEMBER, Role.ADMIN, loginOf(AdminKey.NAME, LoginSource.ADMIN_KEY), "Role of Aurora set to admin by admin."),
						Arguments.of("AURORA", Role.ADMIN, Role.MEMBER, loginOf("Boreas", LoginSource.ACCOUNT), "Role of Aurora set to member by Boreas."));
	}

	@ParameterizedTest
	@MethodSource("changesWithTheLineTheyLog")
	void writesWhichRoleOfWhichAccountWhoSetToTheLog(String asked, Role before, Role role, Login by, String line) {
		accounts.hold(account("Aurora", before));

		tested.to(asked, role, by);

		List<String> logged = logger.infoMessages();
		assertThat(logged).containsExactly(line);
	}

	@Test
	void givesTheAccountTheRoleItWasAskedFor() {
		accounts.hold(account("Aurora", Role.MEMBER));

		RoleChangeOutcome outcome = tested.to("Aurora", Role.ADMIN, THE_ADMIN_KEYS_LOGIN);

		assertThat(accounts.held("aurora")).isEqualTo(account("Aurora", Role.ADMIN));
		assertThat(outcome).isInstanceOf(RoleChangeOutcome.Changed.class);
	}

	@Test
	void endsEveryLoginOfThatAccountAndLeavesTheLoginsOfAnotherOne() {
		accounts.hold(account("Boreas", Role.MEMBER));
		accounts.hold(account("Aurora", Role.MEMBER));
		long before = generations.of("boreas");

		tested.to("Boreas", Role.ADMIN, THE_ADMIN_KEYS_LOGIN);

		long after = generations.of("boreas");
		long other = generations.of("aurora");
		assertThat(after).isEqualTo(before + 1);
		assertThat(other).isZero();
	}

	private static Login loginOf(String accountName, LoginSource source) {
		return new Login(accountName, Role.ADMIN, 0, source, "id", LOGGED_IN, LOGGED_IN.plus(Login.LIFETIME));
	}

	private static Account account(String avatar, Role role) {
		return new Account(avatar, "hash", PasswordState.CHOSEN, role, Optional.empty());
	}
}
