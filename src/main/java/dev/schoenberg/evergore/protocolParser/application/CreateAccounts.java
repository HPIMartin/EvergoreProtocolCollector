package dev.schoenberg.evergore.protocolParser.application;

import java.util.Optional;

import dev.schoenberg.evergore.protocolParser.Logger;
import dev.schoenberg.evergore.protocolParser.businessLogic.KnownAvatars;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.Account;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.LoginName;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.PasswordState;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.Role;

public class CreateAccounts {
	private final KnownAvatars knownAvatars;
	private final AccountRepository accounts;
	private final PasswordGenerator passwordGenerator;
	private final PasswordHasher passwordHasher;
	private final Logger logger;

	public CreateAccounts(KnownAvatars knownAvatars, AccountRepository accounts, PasswordGenerator passwordGenerator, PasswordHasher passwordHasher, Logger logger) {
		this.knownAvatars = knownAvatars;
		this.accounts = accounts;
		this.passwordGenerator = passwordGenerator;
		this.passwordHasher = passwordHasher;
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
		if (nameKey.equals(AdminKey.NAME)) {
			logger.info("No account for the avatar " + avatar + ": the name admin logs in with the admin key.");
			return;
		}
		if (accounts.findByNameKey(nameKey).isPresent()) {
			return;
		}
		Account account = new Account(avatar, passwordHasher.hash(passwordGenerator.generate()), PasswordState.ISSUED, Role.MEMBER, Optional.empty());
		accounts.addIfAbsent(account);
	}
}
