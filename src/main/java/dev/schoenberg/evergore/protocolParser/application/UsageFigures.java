package dev.schoenberg.evergore.protocolParser.application;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

import dev.schoenberg.evergore.protocolParser.businessLogic.Constants;
import dev.schoenberg.evergore.protocolParser.businessLogic.GermanOrder;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.Account;
import dev.schoenberg.evergore.protocolParser.businessLogic.usage.UsageKind;

public class UsageFigures {
	private static final int DAYS_OF_THE_RECENT_WINDOW = 30;
	private static final Instant START_OF_ALL_THE_TIME = Instant.EPOCH;

	private final AccountRepository accounts;
	private final UsageRecordReader records;
	private final LostUsageRecords lost;
	private final Clock clock;

	public UsageFigures(AccountRepository accounts, UsageRecordReader records, LostUsageRecords lost, Clock clock) {
		this.accounts = accounts;
		this.records = records;
		this.lost = lost;
		this.clock = clock;
	}

	public UsageReport report() {
		List<Account> everyAccount = accounts.allAccounts().stream().sorted(Comparator.comparing(Account::avatar, GermanOrder.NAMES)).toList();
		Instant recentStart = clock.instant().atZone(Constants.APP_ZONE).minusDays(DAYS_OF_THE_RECENT_WINDOW).toInstant();
		return new UsageReport(windowFrom(recentStart, everyAccount), windowFrom(START_OF_ALL_THE_TIME, everyAccount),
				records.firstRecord().map(first -> first.atZone(Constants.APP_ZONE).toLocalDate()), lost.lostSinceStart());
	}

	private UsageWindow windowFrom(Instant start, List<Account> everyAccount) {
		Map<String, Map<UsageKind, Integer>> counts = records.countsSince(start);
		Map<String, Set<LocalDate>> days = records.daysUsedSince(start, Constants.APP_ZONE);
		Map<String, Instant> latest = records.latestUses();
		List<AccountUsage> usage = everyAccount.stream().map(account -> new AccountUsage(account.avatar(), countsOf(counts, days, latest, account.nameKey()))).toList();
		return new UsageWindow(usage, guildOf(everyAccount, usage, days), countsOf(counts, days, latest, AdminKey.NAME));
	}

	private static UsageCounts guildOf(List<Account> everyAccount, List<AccountUsage> usage, Map<String, Set<LocalDate>> days) {
		List<UsageCounts> members = usage.stream().map(AccountUsage::counts).toList();
		Set<LocalDate> daysAnyMemberUsedTheApp = everyAccount.stream().flatMap(account -> days.getOrDefault(account.nameKey(), Set.of()).stream()).collect(Collectors.toSet());
		return new UsageCounts(sum(members, UsageCounts::logins), sum(members, UsageCounts::pageLoads), sum(members, UsageCounts::logouts), daysAnyMemberUsedTheApp.size(),
				members.stream().map(UsageCounts::latestUse).flatMap(Optional::stream).max(Comparator.naturalOrder()));
	}

	private static int sum(List<UsageCounts> members, ToIntFunction<UsageCounts> figure) {
		return members.stream().mapToInt(figure).sum();
	}

	private static UsageCounts countsOf(Map<String, Map<UsageKind, Integer>> counts, Map<String, Set<LocalDate>> days, Map<String, Instant> latest, String key) {
		return countsOf(counts.getOrDefault(key, Map.of()), days.getOrDefault(key, Set.of()), Optional.ofNullable(latest.get(key)));
	}

	private static UsageCounts countsOf(Map<UsageKind, Integer> kinds, Set<LocalDate> days, Optional<Instant> latestUse) {
		return new UsageCounts(kinds.getOrDefault(UsageKind.LOGIN, 0), kinds.getOrDefault(UsageKind.PAGE_LOAD, 0), kinds.getOrDefault(UsageKind.LOGOUT, 0), days.size(), latestUse);
	}
}
