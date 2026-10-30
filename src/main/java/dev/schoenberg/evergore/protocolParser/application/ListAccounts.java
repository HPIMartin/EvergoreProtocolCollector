package dev.schoenberg.evergore.protocolParser.application;

import java.util.List;

public class ListAccounts {
	private final AccountRepository accounts;
	private final RestingAccounts restingAccounts;

	public ListAccounts(AccountRepository accounts, RestingAccounts restingAccounts) {
		this.accounts = accounts;
		this.restingAccounts = restingAccounts;
	}

	public List<AccountListing> all() {
		return accounts.allAccounts().stream().map(account -> new AccountListing(account.avatar(), restingAccounts.rest(account), account.lastLogin())).toList();
	}
}
