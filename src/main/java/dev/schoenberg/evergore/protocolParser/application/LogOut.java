package dev.schoenberg.evergore.protocolParser.application;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import dev.schoenberg.evergore.protocolParser.businessLogic.account.Login;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.LoginName;
import dev.schoenberg.evergore.protocolParser.businessLogic.usage.UsageKind;
import dev.schoenberg.evergore.protocolParser.businessLogic.usage.UsageRecord;

public class LogOut {
	private final Map<String, Instant> expiryOfEndedLogins = new ConcurrentHashMap<>();
	private final Clock clock;
	private final UsageRecording usage;

	public LogOut(Clock clock, UsageRecording usage) {
		this.clock = clock;
		this.usage = usage;
	}

	public void of(Login login) {
		Instant now = clock.instant();
		expiryOfEndedLogins.values().removeIf(expiry -> !expiry.isAfter(now));
		if (expiryOfEndedLogins.putIfAbsent(login.id(), login.expiresAt()) == null) {
			usage.record(new UsageRecord(new LoginName(login.accountName()).key(), now, UsageKind.LOGOUT, Optional.empty()));
		}
	}

	public boolean hasEnded(Login login) {
		return expiryOfEndedLogins.containsKey(login.id());
	}
}
