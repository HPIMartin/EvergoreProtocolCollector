package dev.schoenberg.evergore.protocolParser.application;

import java.util.Optional;

import dev.schoenberg.evergore.protocolParser.businessLogic.account.Account;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.PasswordRefusal;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.PasswordRule;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.PasswordState;

public class ChooseNewPassword {
	private final PasswordCheck passwordCheck;
	private final AccountRepository accounts;
	private final PasswordHasher passwordHasher;
	private final PasswordRule passwordRule;
	private final LoginStart loginStart;

	public ChooseNewPassword(PasswordCheck passwordCheck, AccountRepository accounts, PasswordHasher passwordHasher, PasswordRule passwordRule, LoginStart loginStart) {
		this.passwordCheck = passwordCheck;
		this.accounts = accounts;
		this.passwordHasher = passwordHasher;
		this.passwordRule = passwordRule;
		this.loginStart = loginStart;
	}

	public LogInOutcome with(String name, String issuedPassword, String newPassword) {
		long generation = loginStart.generationSeenBeforeTheCheckOf(name);
		Optional<Account> account = passwordCheck.accountWith(name, issuedPassword).filter(found -> found.passwordState() == PasswordState.ISSUED);
		if (account.isEmpty()) {
			return new LogInOutcome.Refused();
		}
		Optional<PasswordRefusal> refusal = passwordRule.refusalOfReplacing(issuedPassword, newPassword);
		if (refusal.isPresent()) {
			return new LogInOutcome.NewPasswordRefused(refusal.get());
		}
		String newHash = passwordHasher.hash(newPassword);
		if (!accounts.replaceIssuedPassword(account.get().nameKey(), newHash)) {
			return new LogInOutcome.Refused();
		}
		return loginStart.startFor(account.get(), generation);
	}
}
