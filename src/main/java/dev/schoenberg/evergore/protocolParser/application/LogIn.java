package dev.schoenberg.evergore.protocolParser.application;

import java.util.Optional;

import dev.schoenberg.evergore.protocolParser.businessLogic.account.Account;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.PasswordState;

public class LogIn {
	private final PasswordCheck passwordCheck;
	private final LoginStart loginStart;

	public LogIn(PasswordCheck passwordCheck, LoginStart loginStart) {
		this.passwordCheck = passwordCheck;
		this.loginStart = loginStart;
	}

	public LogInOutcome with(String name, String password) {
		long generation = loginStart.generationSeenBeforeTheCheckOf(name);
		Optional<Account> account = passwordCheck.accountWith(name, password);
		if (account.isEmpty()) {
			return new LogInOutcome.Refused();
		}
		if (account.get().passwordState() == PasswordState.ISSUED) {
			return new LogInOutcome.ChooseNewPassword();
		}
		return loginStart.startFor(account.get(), generation);
	}
}
