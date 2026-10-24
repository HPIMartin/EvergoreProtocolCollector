package dev.schoenberg.evergore.protocolParser.businessLogic.account;

import java.time.Instant;
import java.util.Optional;

public record Account(String avatar, String passwordHash, PasswordState passwordState, Role role, Optional<Instant> lastLogin) {
	public String nameKey() {
		return new LoginName(avatar).key();
	}
}
