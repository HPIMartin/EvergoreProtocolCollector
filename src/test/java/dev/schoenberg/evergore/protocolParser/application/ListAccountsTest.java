package dev.schoenberg.evergore.protocolParser.application;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import dev.schoenberg.evergore.protocolParser.businessLogic.account.Account;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.PasswordState;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.Role;
import dev.schoenberg.evergore.protocolParser.businessLogic.banking.BankEntry;
import dev.schoenberg.evergore.protocolParser.businessLogic.banking.BankRepositoryStub;
import dev.schoenberg.evergore.protocolParser.businessLogic.base.TransferType;
import dev.schoenberg.evergore.protocolParser.businessLogic.storage.StorageRepositoryStub;

import static org.assertj.core.api.Assertions.assertThat;

class ListAccountsTest {
	private static final Instant LOGGED_IN = Instant.parse("2026-01-05T10:00:00Z");
	private static final Instant NEWEST_MOVEMENT = Instant.parse("2026-01-31T12:00:00Z");

	private final InMemoryAccounts accounts = new InMemoryAccounts(new ArrayList<>());
	private final BankRepositoryStub bank = new BankRepositoryStub();
	private final ListAccounts tested = new ListAccounts(accounts, new RestingAccounts(bank, new StorageRepositoryStub()));

	@Test
	void carriesNoLatestLoginForAnAccountThatNeverLoggedIn() {
		accounts.hold(account("Aurora", Optional.empty()));

		List<AccountListing> listed = tested.all();

		assertThat(listed).containsExactly(new AccountListing("Aurora", false, Optional.empty()));
	}

	@Test
	void carriesTheLatestLoginOfAnAccountThatLoggedIn() {
		accounts.hold(account("Aurora", Optional.of(LOGGED_IN)));

		List<AccountListing> listed = tested.all();

		assertThat(listed).containsExactly(new AccountListing("Aurora", false, Optional.of(LOGGED_IN)));
	}

	@Test
	void marksAnAccountWhoseAvatarMovedNothingForMoreThanThirtyDaysAsResting() {
		accounts.hold(account("Aurora", Optional.empty()));
		accounts.hold(account("Calix", Optional.empty()));
		moved("Aurora", 0);
		moved("Calix", 31);

		List<AccountListing> listed = tested.all();

		assertThat(listed).containsExactly(new AccountListing("Aurora", false, Optional.empty()), new AccountListing("Calix", true, Optional.empty()));
	}

	private void moved(String avatar, int daysBeforeTheNewestMovement) {
		bank.seedEntries(avatar, List.of(new BankEntry(NEWEST_MOVEMENT.minus(Duration.ofDays(daysBeforeTheNewestMovement)), avatar, 300, TransferType.ENTNAHME)));
	}

	private static Account account(String avatar, Optional<Instant> lastLogin) {
		return new Account(avatar, "hash", PasswordState.CHOSEN, Role.MEMBER, lastLogin);
	}
}
