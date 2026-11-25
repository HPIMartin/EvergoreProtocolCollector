package dev.schoenberg.evergore.protocolParser.application;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import dev.schoenberg.evergore.protocolParser.businessLogic.account.Account;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.PasswordState;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.Role;

import static org.assertj.core.api.Assertions.assertThat;

class UsageFiguresTest {
	private static final Instant NOW = Instant.parse("2026-01-10T11:00:00Z");

	private final InMemoryAccounts accounts = new InMemoryAccounts(new ArrayList<>());
	private final CannedUsageReader records = new CannedUsageReader();
	private final UsageFigures tested = new UsageFigures(accounts, records, Clock.fixed(NOW, ZoneOffset.UTC));

	@Test
	void listsEveryAccountInGermanOrderWithNothingCountedWhileItHasNoRecords() {
		accounts.hold(account("Zephyr"));
		accounts.hold(account("Aurora"));
		accounts.hold(account("Ärger"));

		UsageReport report = tested.report();

		assertThat(report.lastThirtyDays().accounts())
				.containsExactly(new AccountUsage("Ärger", UsageCounts.NONE), new AccountUsage("Aurora", UsageCounts.NONE), new AccountUsage("Zephyr", UsageCounts.NONE));
		assertThat(report.allTheTime().accounts()).isEqualTo(report.lastThirtyDays().accounts());
	}

	private static Account account(String avatar) {
		return new Account(avatar, "hashed:" + avatar, PasswordState.CHOSEN, Role.MEMBER, Optional.empty());
	}
}
