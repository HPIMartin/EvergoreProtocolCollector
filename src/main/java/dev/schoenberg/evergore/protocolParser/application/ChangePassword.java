package dev.schoenberg.evergore.protocolParser.application;

import java.util.Optional;
import java.util.OptionalLong;

import dev.schoenberg.evergore.protocolParser.businessLogic.account.Account;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.Login;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.LoginSource;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.PasswordRefusal;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.PasswordRule;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.PasswordState;

public class ChangePassword {
	private final PasswordCheck passwordCheck;
	private final AccountRepository accounts;
	private final PasswordHasher passwordHasher;
	private final PasswordRule passwordRule;
	private final LoginGenerations generations;
	private final SignedLogins signedLogins;

	public ChangePassword(PasswordCheck passwordCheck, AccountRepository accounts, PasswordHasher passwordHasher, PasswordRule passwordRule, LoginGenerations generations,
			SignedLogins signedLogins) {
		this.passwordCheck = passwordCheck;
		this.accounts = accounts;
		this.passwordHasher = passwordHasher;
		this.passwordRule = passwordRule;
		this.generations = generations;
		this.signedLogins = signedLogins;
	}

	public ChangePasswordOutcome with(Login login, String currentPassword, String newPassword) {
		if (login.source() == LoginSource.ADMIN_KEY) {
			return new ChangePasswordOutcome.NoAccount();
		}
		Optional<Account> account = passwordCheck.accountWith(login.accountName(), currentPassword);
		if (account.isEmpty()) {
			return new ChangePasswordOutcome.WrongCurrentPassword();
		}
		Optional<PasswordRefusal> refusal = passwordRule.refusalOfReplacing(currentPassword, newPassword);
		if (refusal.isPresent()) {
			return new ChangePasswordOutcome.NewPasswordRefused(refusal.get());
		}
		String newHash = passwordHasher.hash(newPassword);
		String nameKey = account.get().nameKey();
		OptionalLong generation = generations.bumpWhileAt(nameKey, login.generation(), () -> accounts.replacePassword(nameKey, newHash, PasswordState.CHOSEN));
		if (generation.isEmpty()) {
			return new ChangePasswordOutcome.LoginEnded();
		}
		return new ChangePasswordOutcome.Changed(signedLogins.issue(account.get().avatar(), login.role(), generation.getAsLong(), login.expiresAt()));
	}
}
