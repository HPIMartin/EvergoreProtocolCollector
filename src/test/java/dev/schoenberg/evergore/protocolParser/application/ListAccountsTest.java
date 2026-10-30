package dev.schoenberg.evergore.protocolParser.application;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import dev.schoenberg.evergore.protocolParser.businessLogic.account.Account;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.PasswordState;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.Role;

import static org.assertj.core.api.Assertions.assertThat;

class ListAccountsTest {
	private static final Instant LOGGED_IN = Instant.parse("2026-01-05T10:00:00Z");

	private final InMemoryAccounts accounts = new InMemoryAccounts(new ArrayList<>());
	private final ListAccounts tested = new ListAccounts(accounts);

	@Test
	void carriesNoLatestLoginForAnAccountThatNeverLoggedIn() {
		accounts.hold(new Account("Aurora", "hash", PasswordState.ISSUED, Role.MEMBER, Optional.empty()));

		List<AccountListing> listed = tested.all();

		assertThat(listed).containsExactly(new AccountListing("Aurora", false, Optional.empty()));
	}

	@Test
	void carriesTheLatestLoginOfAnAccountThatLoggedIn() {
		accounts.hold(new Account("Aurora", "hash", PasswordState.CHOSEN, Role.MEMBER, Optional.of(LOGGED_IN)));

		List<AccountListing> listed = tested.all();

		assertThat(listed).containsExactly(new AccountListing("Aurora", false, Optional.of(LOGGED_IN)));
	}
}
