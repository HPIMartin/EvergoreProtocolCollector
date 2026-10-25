package dev.schoenberg.evergore.protocolParser.application;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import dev.schoenberg.evergore.protocolParser.businessLogic.account.Login;

public class LogOut {
	private final Map<String, Instant> expiryOfEndedLogins = new ConcurrentHashMap<>();
	private final Clock clock;

	public LogOut(Clock clock) {
		this.clock = clock;
	}

	public void of(Login login) {
		Instant now = clock.instant();
		expiryOfEndedLogins.values().removeIf(expiry -> !expiry.isAfter(now));
		expiryOfEndedLogins.put(login.id(), login.expiresAt());
	}

	public boolean hasEnded(Login login) {
		return expiryOfEndedLogins.containsKey(login.id());
	}
}
