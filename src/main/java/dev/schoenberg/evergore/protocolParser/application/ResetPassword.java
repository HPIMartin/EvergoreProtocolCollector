package dev.schoenberg.evergore.protocolParser.application;

import dev.schoenberg.evergore.protocolParser.businessLogic.account.LoginName;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.PasswordState;

public class ResetPassword {
	private final AccountRepository accounts;
	private final PasswordGenerator passwordGenerator;
	private final PasswordHasher passwordHasher;

	public ResetPassword(AccountRepository accounts, PasswordGenerator passwordGenerator, PasswordHasher passwordHasher) {
		this.accounts = accounts;
		this.passwordGenerator = passwordGenerator;
		this.passwordHasher = passwordHasher;
	}

	public void forAccount(String name) {
		String issued = passwordGenerator.generate();
		accounts.replacePassword(new LoginName(name).key(), passwordHasher.hash(issued), PasswordState.ISSUED);
	}
}
