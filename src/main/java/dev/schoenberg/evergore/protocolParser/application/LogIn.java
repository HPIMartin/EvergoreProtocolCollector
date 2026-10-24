package dev.schoenberg.evergore.protocolParser.application;

import java.time.Clock;
import java.util.Optional;

import dev.schoenberg.evergore.protocolParser.businessLogic.account.Account;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.LoginName;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.PasswordState;

public class LogIn {
	private final AccountRepository accounts;
	private final PasswordHasher passwordHasher;
	private final SignedLogins signedLogins;
	private final LoginGenerations generations;
	private final Clock clock;
	private final String hashOfNoPassword;

	public LogIn(AccountRepository accounts, PasswordHasher passwordHasher, SignedLogins signedLogins, LoginGenerations generations, Clock clock, String hashOfNoPassword) {
		this.accounts = accounts;
		this.passwordHasher = passwordHasher;
		this.signedLogins = signedLogins;
		this.generations = generations;
		this.clock = clock;
		this.hashOfNoPassword = hashOfNoPassword;
	}

	public LogInOutcome with(String name, String password) {
		String nameKey = new LoginName(name).key();
		long generationSeenBeforeTheCheck = generations.of(nameKey);
		Optional<Account> account = accounts.findByNameKey(nameKey);
		boolean rightPassword = passwordHasher.verify(password, account.map(Account::passwordHash).orElse(hashOfNoPassword));
		if (account.isEmpty() || !rightPassword) {
			return new LogInOutcome.Refused();
		}
		if (account.get().passwordState() == PasswordState.ISSUED) {
			return new LogInOutcome.ChooseNewPassword();
		}
		return loggedIn(account.get(), generationSeenBeforeTheCheck);
	}

	private LogInOutcome loggedIn(Account account, long generationSeenBeforeTheCheck) {
		accounts.recordLastLogin(account.nameKey(), clock.instant());
		return new LogInOutcome.LoggedIn(signedLogins.issue(account.avatar(), account.role(), generationSeenBeforeTheCheck));
	}
}
