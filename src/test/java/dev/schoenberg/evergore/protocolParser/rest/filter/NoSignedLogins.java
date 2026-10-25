package dev.schoenberg.evergore.protocolParser.rest.filter;

import java.util.Optional;

import dev.schoenberg.evergore.protocolParser.application.SignedLogins;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.Login;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.Role;

final class NoSignedLogins implements SignedLogins {
	@Override
	public String issue(String accountName, Role role, long generation) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String issueForTheAdminKey(String name) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Optional<Login> read(String value) {
		return Optional.empty();
	}
}
