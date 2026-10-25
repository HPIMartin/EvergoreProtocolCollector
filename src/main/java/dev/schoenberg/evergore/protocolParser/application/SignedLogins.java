package dev.schoenberg.evergore.protocolParser.application;

import java.time.Instant;
import java.util.Optional;

import dev.schoenberg.evergore.protocolParser.businessLogic.account.Login;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.Role;

public interface SignedLogins {
	String issue(String accountName, Role role, long generation);

	String issue(String accountName, Role role, long generation, Instant expiresAt);

	String issueForTheAdminKey(String name);

	Optional<Login> read(String value);
}
