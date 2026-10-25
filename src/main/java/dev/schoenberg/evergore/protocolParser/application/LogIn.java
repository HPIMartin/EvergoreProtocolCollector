package dev.schoenberg.evergore.protocolParser.application;

import java.util.Optional;

import dev.schoenberg.evergore.protocolParser.businessLogic.account.Account;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.PasswordState;

public class LogIn {
	private final PasswordCheck passwordCheck;
	private final LoginStart loginStart;
	private final AdminKey adminKey;
	private final RestingAccounts restingAccounts;

	public LogIn(PasswordCheck passwordCheck, LoginStart loginStart, AdminKey adminKey, RestingAccounts restingAccounts) {
		this.restingAccounts = restingAccounts;
		this.passwordCheck = passwordCheck;
		this.loginStart = loginStart;
		this.adminKey = adminKey;
	}

	public LogInOutcome with(String name, String password) {
		if (adminKey.isNamedBy(name)) {
			return adminKey.opens(name, password) ? loginStart.startForTheAdminKey() : new LogInOutcome.Refused();
		}
		long generation = loginStart.generationSeenBeforeTheCheckOf(name);
		Optional<Account> account = passwordCheck.accountWith(name, password);
		if (account.isEmpty()) {
			return new LogInOutcome.Refused();
		}
		if (restingAccounts.rest(account.get())) {
			return new LogInOutcome.Resting();
		}
		if (account.get().passwordState() == PasswordState.ISSUED) {
			return new LogInOutcome.ChooseNewPassword();
		}
		return loginStart.startFor(account.get(), generation);
	}
}
