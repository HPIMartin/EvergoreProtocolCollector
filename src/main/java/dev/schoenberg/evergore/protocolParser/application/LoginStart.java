package dev.schoenberg.evergore.protocolParser.application;

import java.time.Clock;
import java.util.Optional;

import dev.schoenberg.evergore.protocolParser.businessLogic.account.Account;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.LoginName;
import dev.schoenberg.evergore.protocolParser.businessLogic.usage.UsageKind;
import dev.schoenberg.evergore.protocolParser.businessLogic.usage.UsageRecord;

public class LoginStart {
	private final AccountRepository accounts;
	private final SignedLogins signedLogins;
	private final LoginGenerations generations;
	private final Clock clock;
	private final UsageRecording usage;

	public LoginStart(AccountRepository accounts, SignedLogins signedLogins, LoginGenerations generations, Clock clock, UsageRecording usage) {
		this.accounts = accounts;
		this.signedLogins = signedLogins;
		this.generations = generations;
		this.clock = clock;
		this.usage = usage;
	}

	public long generationSeenBeforeTheCheckOf(String name) {
		return generations.of(new LoginName(name).key());
	}

	public LogInOutcome startFor(Account account, long generationSeenBeforeTheCheck) {
		return new LogInOutcome.LoggedIn(loginValueFor(account, generationSeenBeforeTheCheck));
	}

	private String loginValueFor(Account account, long generationSeenBeforeTheCheck) {
		if (generations.of(account.nameKey()) == generationSeenBeforeTheCheck) {
			accounts.recordLastLogin(account.nameKey(), clock.instant());
		}
		usage.record(new UsageRecord(account.nameKey(), clock.instant(), UsageKind.LOGIN, Optional.empty()));
		return signedLogins.issue(account.avatar(), account.role(), generationSeenBeforeTheCheck);
	}

	public LogInOutcome startForTheAdminKey() {
		return new LogInOutcome.LoggedIn(signedLogins.issueForTheAdminKey(AdminKey.NAME));
	}
}
