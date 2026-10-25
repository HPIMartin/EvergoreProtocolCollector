package dev.schoenberg.evergore.protocolParser.application;

import java.util.Optional;

import dev.schoenberg.evergore.protocolParser.businessLogic.account.Login;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.LoginName;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.LoginSource;

public class ValidLogins {
	private final SignedLogins signedLogins;
	private final LoginGenerations generations;

	public ValidLogins(SignedLogins signedLogins, LoginGenerations generations) {
		this.signedLogins = signedLogins;
		this.generations = generations;
	}

	public Optional<Login> behind(String value) {
		return signedLogins.read(value).filter(this::isCurrent);
	}

	private boolean isCurrent(Login login) {
		return login.source() == LoginSource.ADMIN_KEY || login.generation() == generations.of(new LoginName(login.accountName()).key());
	}
}
