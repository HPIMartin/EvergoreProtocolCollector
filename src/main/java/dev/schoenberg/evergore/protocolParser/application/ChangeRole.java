package dev.schoenberg.evergore.protocolParser.application;

import dev.schoenberg.evergore.protocolParser.businessLogic.account.Login;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.LoginName;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.Role;

public class ChangeRole {
	private final AccountRepository accounts;

	public ChangeRole(AccountRepository accounts) {
		this.accounts = accounts;
	}

	public RoleChangeOutcome to(String name, Role role, Login by) {
		accounts.replaceRole(new LoginName(name).key(), role);
		return new RoleChangeOutcome.Changed();
	}
}
