package dev.schoenberg.evergore.protocolParser.application;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import dev.schoenberg.evergore.protocolParser.businessLogic.account.Account;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.LoginName;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.PasswordState;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.Role;

import static org.assertj.core.api.Assertions.assertThat;

class ResetPasswordTest {
	private static final Instant LOGGED_IN = Instant.parse("2026-01-05T10:00:00Z");

	private final InMemoryAccounts accounts = new InMemoryAccounts(new ArrayList<>());
	private final LoginGenerations generations = new LoginGenerations();
	private final AtomicInteger generated = new AtomicInteger();
	private final PasswordGenerator generator = () -> "Generated-" + generated.incrementAndGet() + "!";
	private final PrefixHasher hasher = new PrefixHasher(new ArrayList<>());
	private final ResetPassword tested = new ResetPassword(accounts, generator, hasher, generations);

	@Test
	void storesOnlyTheHashOfANewRandomPasswordAsIssued() {
		accounts.hold(new Account("Boreas", "hashed:Winter-2026!", PasswordState.CHOSEN, Role.MEMBER, Optional.of(LOGGED_IN)));

		tested.forAccount("Boreas");

		assertThat(accounts.held("boreas")).isEqualTo(new Account("Boreas", "hashed:Generated-1!", PasswordState.ISSUED, Role.MEMBER, Optional.of(LOGGED_IN)));
	}

	@Test
	void returnsTheClearPasswordItIssuedToItsCaller() {
		accounts.hold(new Account("Boreas", "hashed:Winter-2026!", PasswordState.CHOSEN, Role.MEMBER, Optional.of(LOGGED_IN)));

		Optional<String> issued = tested.forAccount("Boreas");

		assertThat(issued).hasValue("Generated-1!");
	}

	@Test
	void endsEveryLoginOfThatAccountAndLeavesTheLoginsOfAnotherOne() {
		accounts.hold(new Account("Boreas", "hashed:Winter-2026!", PasswordState.CHOSEN, Role.MEMBER, Optional.of(LOGGED_IN)));
		accounts.hold(new Account("Aurora", "hashed:Sommer-2026!", PasswordState.CHOSEN, Role.MEMBER, Optional.of(LOGGED_IN)));
		long before = generations.of("boreas");

		tested.forAccount("Boreas");

		long after = generations.of("boreas");
		long other = generations.of("aurora");
		assertThat(after).isEqualTo(before + 1);
		assertThat(other).isZero();
	}

	@Test
	void givesTwoResetsTwoDifferentPasswordsAndOnlyTheLatestChecks() {
		accounts.hold(new Account("Boreas", "hashed:Winter-2026!", PasswordState.CHOSEN, Role.MEMBER, Optional.of(LOGGED_IN)));
		PasswordCheck check = new PasswordCheck(accounts, hasher, "hashed:a password nobody holds");

		String first = tested.forAccount("Boreas").orElseThrow();
		String second = tested.forAccount("Boreas").orElseThrow();

		Optional<Account> withTheFirst = check.accountWith("Boreas", first);
		Optional<Account> withTheSecond = check.accountWith("Boreas", second);
		assertThat(second).isNotEqualTo(first);
		assertThat(withTheFirst).isEmpty();
		assertThat(withTheSecond).isPresent();
	}

	@ParameterizedTest
	@ValueSource(strings = {"Zephyr", " zephyr ", "admin", "Admin"})
	void refusesANameWithoutAnAccountAndCreatesNone(String name) {
		accounts.hold(new Account("Boreas", "hashed:Winter-2026!", PasswordState.CHOSEN, Role.MEMBER, Optional.of(LOGGED_IN)));

		Optional<String> issued = tested.forAccount(name);

		assertThat(issued).isEmpty();
		assertThat(accounts.allAccounts()).extracting(Account::avatar).containsExactly("Boreas");
		assertThat(generations.of(new LoginName(name).key())).isZero();
	}
}
