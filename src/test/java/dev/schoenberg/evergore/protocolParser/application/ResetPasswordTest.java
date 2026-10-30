package dev.schoenberg.evergore.protocolParser.application;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import dev.schoenberg.evergore.protocolParser.businessLogic.account.Account;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.PasswordState;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.Role;

import static org.assertj.core.api.Assertions.assertThat;

class ResetPasswordTest {
	private static final Instant LOGGED_IN = Instant.parse("2026-01-05T10:00:00Z");

	private final InMemoryAccounts accounts = new InMemoryAccounts(new ArrayList<>());
	private final AtomicInteger generated = new AtomicInteger();
	private final PasswordGenerator generator = () -> "Generated-" + generated.incrementAndGet() + "!";
	private final ResetPassword tested = new ResetPassword(accounts, generator, new PrefixHasher(new ArrayList<>()));

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
}
