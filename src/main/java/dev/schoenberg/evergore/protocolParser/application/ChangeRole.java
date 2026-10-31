package dev.schoenberg.evergore.protocolParser.application;

import dev.schoenberg.evergore.protocolParser.businessLogic.account.Login;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.LoginName;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.Role;

public class ChangeRole {
	private final AccountRepository accounts;
	private final LoginGenerations generations;

	public ChangeRole(AccountRepository accounts, LoginGenerations generations) {
		this.accounts = accounts;
		this.generations = generations;
	}

	public RoleChangeOutcome to(String name, Role role, Login by) {
		String nameKey = new LoginName(name).key();
		generations.bumpIf(nameKey, () -> accounts.replaceRole(nameKey, role));
		return new RoleChangeOutcome.Changed();
	}
}
