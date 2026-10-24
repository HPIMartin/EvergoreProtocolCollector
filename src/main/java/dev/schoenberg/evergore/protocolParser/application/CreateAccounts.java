package dev.schoenberg.evergore.protocolParser.application;

import java.util.Optional;

import dev.schoenberg.evergore.protocolParser.Logger;
import dev.schoenberg.evergore.protocolParser.businessLogic.KnownAvatars;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.Account;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.LoginName;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.PasswordState;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.Role;

public class CreateAccounts {
	private static final String ADMIN_KEY_NAME = "admin";

	private final KnownAvatars knownAvatars;
	private final AccountRepository accounts;
	private final PasswordGenerator passwordGenerator;
	private final PasswordHasher passwordHasher;
	private final IssuedPasswords issuedPasswords;
	private final Logger logger;

	public CreateAccounts(KnownAvatars knownAvatars, AccountRepository accounts, PasswordGenerator passwordGenerator, PasswordHasher passwordHasher,
			IssuedPasswords issuedPasswords, Logger logger) {
		this.knownAvatars = knownAvatars;
		this.accounts = accounts;
		this.passwordGenerator = passwordGenerator;
		this.passwordHasher = passwordHasher;
		this.issuedPasswords = issuedPasswords;
		this.logger = logger;
	}

	public void forEveryKnownAvatar() {
		knownAvatars.sortedByName().forEach(this::createAccountForOneAvatarAlone);
	}

	private void createAccountForOneAvatarAlone(String avatar) {
		try {
			createAccountFor(avatar);
		} catch (RuntimeException e) {
			logger.error("The account of the avatar " + avatar + " could not be created; the other avatars still get theirs.", e);
		}
	}

	private void createAccountFor(String avatar) {
		String nameKey = new LoginName(avatar).key();
		if (nameKey.equals(ADMIN_KEY_NAME)) {
			logger.info("No account for the avatar " + avatar + ": the name admin logs in with the admin key.");
			return;
		}
		if (accounts.findByNameKey(nameKey).isPresent()) {
			return;
		}
		String issued = passwordGenerator.generate();
		Account account = new Account(avatar, passwordHasher.hash(issued), PasswordState.ISSUED, Role.MEMBER, Optional.empty());
		if (accounts.addIfAbsent(account)) {
			issuedPasswords.issue(avatar, issued);
		}
	}
}
