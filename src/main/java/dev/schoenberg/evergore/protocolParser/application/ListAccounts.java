package dev.schoenberg.evergore.protocolParser.application;

import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

import dev.schoenberg.evergore.protocolParser.businessLogic.GermanOrder;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.Account;

public class ListAccounts {
	private final AccountRepository accounts;
	private final RestingAccounts restingAccounts;

	public ListAccounts(AccountRepository accounts, RestingAccounts restingAccounts) {
		this.accounts = accounts;
		this.restingAccounts = restingAccounts;
	}

	public List<AccountListing> all() {
		Predicate<Account> rests = restingAccounts.forOneListing();
		return accounts
				.allAccounts()
				.stream()
				.map(account -> new AccountListing(account.avatar(), rests.test(account), account.lastLogin()))
				.sorted(Comparator.comparing(AccountListing::name, GermanOrder.NAMES))
				.toList();
	}
}
