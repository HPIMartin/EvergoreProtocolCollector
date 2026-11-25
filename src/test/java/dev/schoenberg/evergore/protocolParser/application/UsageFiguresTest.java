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
import dev.schoenberg.evergore.protocolParser.businessLogic.usage.UsageKind;

import static org.assertj.core.api.Assertions.assertThat;

class UsageFiguresTest {
	private static final Instant NOW = Instant.parse("2026-04-05T10:00:00Z");
	private static final Instant SAME_BERLIN_TIME_THIRTY_DAYS_BACK = Instant.parse("2026-03-06T11:00:00Z");
	private static final Instant ALL_THE_TIME = Instant.EPOCH;

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

	@Test
	void countsTheLoginsPageLoadsAndLogoutsOfEachAccountFromTheSameBerlinTimeThirtyDaysBackAndOverAllTheTime() {
		accounts.hold(account("Aurora"));
		records.counts(SAME_BERLIN_TIME_THIRTY_DAYS_BACK, "aurora", UsageKind.LOGIN, 2);
		records.counts(SAME_BERLIN_TIME_THIRTY_DAYS_BACK, "aurora", UsageKind.PAGE_LOAD, 5);
		records.counts(SAME_BERLIN_TIME_THIRTY_DAYS_BACK, "aurora", UsageKind.LOGOUT, 1);
		records.counts(ALL_THE_TIME, "aurora", UsageKind.LOGIN, 3);
		records.counts(ALL_THE_TIME, "aurora", UsageKind.PAGE_LOAD, 7);
		records.counts(ALL_THE_TIME, "aurora", UsageKind.LOGOUT, 2);

		UsageReport report = tested.report();

		assertThat(report.lastThirtyDays().accounts()).containsExactly(new AccountUsage("Aurora", new UsageCounts(2, 5, 1, 0, Optional.empty())));
		assertThat(report.allTheTime().accounts()).containsExactly(new AccountUsage("Aurora", new UsageCounts(3, 7, 2, 0, Optional.empty())));
	}

	private static Account account(String avatar) {
		return new Account(avatar, "hashed:" + avatar, PasswordState.CHOSEN, Role.MEMBER, Optional.empty());
	}
}
