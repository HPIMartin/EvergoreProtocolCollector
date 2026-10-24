package dev.schoenberg.evergore.protocolParser.application;

import java.util.Optional;

import dev.schoenberg.evergore.protocolParser.businessLogic.account.Login;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.Role;

public interface SignedLogins {
	String issue(String accountName, Role role, long generation);

	Optional<Login> read(String value);
}
