package dev.schoenberg.evergore.protocolParser.rest.filter;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import static java.time.Instant.EPOCH;

class LoginPot {
	private final long sequence;
	private final Duration count;
	private final Set<String> addresses = new HashSet<>();
	private final Set<String> accounts = new HashSet<>();
	private Instant countStart;
	private long requests = 0;
	private Instant turnedAwayUntil = EPOCH;

	LoginPot(long sequence, Duration count, Instant countStart) {
		this.sequence = sequence;
		this.count = count;
		this.countStart = countStart;
	}

	long sequence() {
		return sequence;
	}

	Set<String> addresses() {
		return addresses;
	}

	Set<String> accounts() {
		return accounts;
	}

	int members() {
		return addresses.size() + accounts.size();
	}

	long countRequest() {
		return ++requests;
	}

	void turnAwayUntil(Instant end) {
		turnedAwayUntil = end;
		requests = 0;
	}

	boolean isTurnedAwayAt(Instant moment) {
		return moment.isBefore(turnedAwayUntil);
	}

	Instant forgottenAt() {
		return turnedAwayUntil.isAfter(EPOCH) ? turnedAwayUntil : countStart.plus(count);
	}

	void absorb(LoginPot other) {
		addresses.addAll(other.addresses);
		accounts.addAll(other.accounts);
		requests += other.requests;
		countStart = countStart.isBefore(other.countStart) ? countStart : other.countStart;
		turnedAwayUntil = turnedAwayUntil.isAfter(other.turnedAwayUntil) ? turnedAwayUntil : other.turnedAwayUntil;
	}
}
