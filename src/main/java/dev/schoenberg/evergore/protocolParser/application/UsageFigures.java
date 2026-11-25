package dev.schoenberg.evergore.protocolParser.application;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import dev.schoenberg.evergore.protocolParser.businessLogic.Constants;
import dev.schoenberg.evergore.protocolParser.businessLogic.GermanOrder;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.Account;
import dev.schoenberg.evergore.protocolParser.businessLogic.usage.UsageKind;

public class UsageFigures {
	private static final int DAYS_OF_THE_RECENT_WINDOW = 30;
	private static final Instant START_OF_ALL_THE_TIME = Instant.EPOCH;

	private final AccountRepository accounts;
	private final UsageRecordReader records;
	private final Clock clock;

	public UsageFigures(AccountRepository accounts, UsageRecordReader records, Clock clock) {
		this.accounts = accounts;
		this.records = records;
		this.clock = clock;
	}

	public UsageReport report() {
		List<Account> everyAccount = accounts.allAccounts().stream().sorted(Comparator.comparing(Account::avatar, GermanOrder.NAMES)).toList();
		Instant recentStart = clock.instant().atZone(Constants.APP_ZONE).minusDays(DAYS_OF_THE_RECENT_WINDOW).toInstant();
		return new UsageReport(windowFrom(recentStart, everyAccount), windowFrom(START_OF_ALL_THE_TIME, everyAccount));
	}

	private UsageWindow windowFrom(Instant start, List<Account> everyAccount) {
		Map<String, Map<UsageKind, Integer>> counts = records.countsSince(start);
		Map<String, Set<LocalDate>> days = records.daysUsedSince(start, Constants.APP_ZONE);
		Map<String, Instant> latest = records.latestUses();
		return new UsageWindow(everyAccount.stream().map(account -> new AccountUsage(account.avatar(), countsOf(counts, days, latest, account.nameKey()))).toList());
	}

	private static UsageCounts countsOf(Map<String, Map<UsageKind, Integer>> counts, Map<String, Set<LocalDate>> days, Map<String, Instant> latest, String key) {
		return countsOf(counts.getOrDefault(key, Map.of()), days.getOrDefault(key, Set.of()), Optional.ofNullable(latest.get(key)));
	}

	private static UsageCounts countsOf(Map<UsageKind, Integer> kinds, Set<LocalDate> days, Optional<Instant> latestUse) {
		return new UsageCounts(kinds.getOrDefault(UsageKind.LOGIN, 0), kinds.getOrDefault(UsageKind.PAGE_LOAD, 0), kinds.getOrDefault(UsageKind.LOGOUT, 0), days.size(), latestUse);
	}
}
