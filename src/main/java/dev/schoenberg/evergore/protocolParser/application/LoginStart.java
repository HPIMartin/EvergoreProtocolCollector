package dev.schoenberg.evergore.protocolParser.application;

import java.time.Clock;

import dev.schoenberg.evergore.protocolParser.businessLogic.account.Account;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.LoginName;

public class LoginStart {
	private final AccountRepository accounts;
	private final SignedLogins signedLogins;
	private final LoginGenerations generations;
	private final Clock clock;

	public LoginStart(AccountRepository accounts, SignedLogins signedLogins, LoginGenerations generations, Clock clock) {
		this.accounts = accounts;
		this.signedLogins = signedLogins;
		this.generations = generations;
		this.clock = clock;
	}

	public long generationSeenBeforeTheCheckOf(String name) {
		return generations.of(new LoginName(name).key());
	}

	public LogInOutcome startFor(Account account, long generationSeenBeforeTheCheck) {
		accounts.recordLastLogin(account.nameKey(), clock.instant());
		return new LogInOutcome.LoggedIn(signedLogins.issue(account.avatar(), account.role(), generationSeenBeforeTheCheck));
	}

	public LogInOutcome startForTheAdminKey() {
		return new LogInOutcome.LoggedIn(signedLogins.issueForTheAdminKey(AdminKey.NAME));
	}
}
