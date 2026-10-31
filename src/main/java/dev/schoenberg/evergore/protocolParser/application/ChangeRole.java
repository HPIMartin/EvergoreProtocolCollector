package dev.schoenberg.evergore.protocolParser.application;

import java.util.Locale;

import dev.schoenberg.evergore.protocolParser.Logger;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.Account;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.Login;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.LoginName;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.Role;

public class ChangeRole {
	private final AccountRepository accounts;
	private final LoginGenerations generations;
	private final Logger logger;

	public ChangeRole(AccountRepository accounts, LoginGenerations generations, Logger logger) {
		this.accounts = accounts;
		this.generations = generations;
		this.logger = logger;
	}

	public RoleChangeOutcome to(String name, Role role, Login by) {
		String nameKey = new LoginName(name).key();
		generations.bumpIf(nameKey, () -> accounts.replaceRole(nameKey, role));
		accounts.findByNameKey(nameKey).ifPresent(account -> logger.info(lineOf(account, role, by)));
		return new RoleChangeOutcome.Changed();
	}

	private static String lineOf(Account account, Role role, Login by) {
		return "Role of " + account.avatar() + " set to " + role.name().toLowerCase(Locale.ROOT) + " by " + by.accountName() + ".";
	}
}
