package dev.schoenberg.evergore.protocolParser.application;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
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

	@Test
	void countsTheBerlinDaysOfEachAccountInEachWindowAndGivesItsLatestUseOverAllTheTimeInBoth() {
		accounts.hold(account("Aurora"));
		records.days(SAME_BERLIN_TIME_THIRTY_DAYS_BACK, "aurora", LocalDate.parse("2026-04-01"), LocalDate.parse("2026-04-03"));
		records.days(ALL_THE_TIME, "aurora", LocalDate.parse("2026-02-01"), LocalDate.parse("2026-04-01"), LocalDate.parse("2026-04-03"));
		Instant latest = Instant.parse("2026-04-03T19:15:00Z");
		records.latest("aurora", latest);

		UsageReport report = tested.report();

		assertThat(report.lastThirtyDays().accounts()).containsExactly(new AccountUsage("Aurora", new UsageCounts(0, 0, 0, 2, Optional.of(latest))));
		assertThat(report.allTheTime().accounts()).containsExactly(new AccountUsage("Aurora", new UsageCounts(0, 0, 0, 3, Optional.of(latest))));
	}

	@Test
	void addsUpTheAccountsInTheGuildRowCountingTheDaysAnyMemberUsedTheAppAndLeavingTheAdminKeyOut() {
		accounts.hold(account("Aurora"));
		accounts.hold(account("Boreas"));
		records.counts(SAME_BERLIN_TIME_THIRTY_DAYS_BACK, "aurora", UsageKind.LOGIN, 2);
		records.counts(SAME_BERLIN_TIME_THIRTY_DAYS_BACK, "aurora", UsageKind.PAGE_LOAD, 3);
		records.counts(SAME_BERLIN_TIME_THIRTY_DAYS_BACK, "aurora", UsageKind.LOGOUT, 1);
		records.counts(SAME_BERLIN_TIME_THIRTY_DAYS_BACK, "boreas", UsageKind.LOGIN, 1);
		records.counts(SAME_BERLIN_TIME_THIRTY_DAYS_BACK, "boreas", UsageKind.PAGE_LOAD, 4);
		records.counts(SAME_BERLIN_TIME_THIRTY_DAYS_BACK, AdminKey.NAME, UsageKind.LOGIN, 5);
		records.days(SAME_BERLIN_TIME_THIRTY_DAYS_BACK, "aurora", LocalDate.parse("2026-04-01"), LocalDate.parse("2026-04-03"));
		records.days(SAME_BERLIN_TIME_THIRTY_DAYS_BACK, "boreas", LocalDate.parse("2026-04-03"), LocalDate.parse("2026-04-04"));
		records.days(SAME_BERLIN_TIME_THIRTY_DAYS_BACK, AdminKey.NAME, LocalDate.parse("2026-04-02"));
		Instant latestOfAMember = Instant.parse("2026-04-04T08:00:00Z");
		records.latest("aurora", Instant.parse("2026-04-03T19:15:00Z"));
		records.latest("boreas", latestOfAMember);
		records.latest(AdminKey.NAME, Instant.parse("2026-04-05T09:00:00Z"));

		UsageReport report = tested.report();

		assertThat(report.lastThirtyDays().guild()).isEqualTo(new UsageCounts(3, 7, 1, 3, Optional.of(latestOfAMember)));
		assertThat(report.allTheTime().guild()).isEqualTo(new UsageCounts(0, 0, 0, 0, Optional.of(latestOfAMember)));
	}

	private static Account account(String avatar) {
		return new Account(avatar, "hashed:" + avatar, PasswordState.CHOSEN, Role.MEMBER, Optional.empty());
	}
}
