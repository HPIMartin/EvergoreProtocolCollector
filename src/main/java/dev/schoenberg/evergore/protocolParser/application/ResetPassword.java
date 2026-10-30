package dev.schoenberg.evergore.protocolParser.application;

import java.util.Optional;

import dev.schoenberg.evergore.protocolParser.businessLogic.account.LoginName;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.PasswordState;

public class ResetPassword {
	private final AccountRepository accounts;
	private final PasswordGenerator passwordGenerator;
	private final PasswordHasher passwordHasher;
	private final LoginGenerations generations;

	public ResetPassword(AccountRepository accounts, PasswordGenerator passwordGenerator, PasswordHasher passwordHasher, LoginGenerations generations) {
		this.accounts = accounts;
		this.passwordGenerator = passwordGenerator;
		this.passwordHasher = passwordHasher;
		this.generations = generations;
	}

	public Optional<String> forAccount(String name) {
		String nameKey = new LoginName(name).key();
		String issued = passwordGenerator.generate();
		String hash = passwordHasher.hash(issued);
		generations.bump(nameKey, () -> accounts.replacePassword(nameKey, hash, PasswordState.ISSUED));
		return Optional.of(issued);
	}
}
