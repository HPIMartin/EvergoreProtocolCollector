package dev.schoenberg.evergore.protocolParser.application;

import java.time.Clock;
import java.util.List;

import dev.schoenberg.evergore.protocolParser.businessLogic.GermanOrder;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.Account;

public class UsageFigures {
	private final AccountRepository accounts;
	private final UsageRecordReader records;
	private final Clock clock;

	public UsageFigures(AccountRepository accounts, UsageRecordReader records, Clock clock) {
		this.accounts = accounts;
		this.records = records;
		this.clock = clock;
	}

	public UsageReport report() {
		List<AccountUsage> everyAccount = accounts
				.allAccounts()
				.stream()
				.map(Account::avatar)
				.sorted(GermanOrder.NAMES)
				.map(name -> new AccountUsage(name, UsageCounts.NONE))
				.toList();
		return new UsageReport(new UsageWindow(everyAccount), new UsageWindow(everyAccount));
	}
}
