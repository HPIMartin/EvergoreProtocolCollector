package dev.schoenberg.evergore.protocolParser.application;

import java.util.List;

public class ListAccounts {
	private final AccountRepository accounts;

	public ListAccounts(AccountRepository accounts) {
		this.accounts = accounts;
	}

	public List<AccountListing> all() {
		return accounts.allAccounts().stream().map(account -> new AccountListing(account.avatar(), false, account.lastLogin())).toList();
	}
}
