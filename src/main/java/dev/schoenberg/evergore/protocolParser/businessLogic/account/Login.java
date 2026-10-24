package dev.schoenberg.evergore.protocolParser.businessLogic.account;

import java.time.Duration;
import java.time.Instant;

public record Login(String accountName, Role role, long generation, String id, Instant issuedAt, Instant expiresAt) {
	public static final Duration LIFETIME = Duration.ofDays(7);
}
