package dev.schoenberg.evergore.protocolParser.application;

import java.util.Optional;

import dev.schoenberg.evergore.protocolParser.businessLogic.account.Account;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.LoginName;

public class PasswordCheck {
	private final AccountRepository accounts;
	private final PasswordHasher passwordHasher;
	private final String hashOfNoPassword;

	public PasswordCheck(AccountRepository accounts, PasswordHasher passwordHasher, String hashOfNoPassword) {
		this.accounts = accounts;
		this.passwordHasher = passwordHasher;
		this.hashOfNoPassword = hashOfNoPassword;
	}

	public Optional<Account> accountWith(String name, String password) {
		Optional<Account> account = accounts.findByNameKey(new LoginName(name).key());
		boolean rightPassword = passwordHasher.verify(password, account.map(Account::passwordHash).orElse(hashOfNoPassword));
		return account.filter(_ -> rightPassword);
	}
}
