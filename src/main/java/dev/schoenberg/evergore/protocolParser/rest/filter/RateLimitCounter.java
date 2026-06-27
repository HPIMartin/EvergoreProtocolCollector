package dev.schoenberg.evergore.protocolParser.rest.filter;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import static java.time.Duration.between;
import static java.time.Instant.EPOCH;

class RateLimitCounter {
	private final Duration interval;
	private final Duration blockDuration;
	private final Clock clock;
	private long count = 0;
	private Instant lastReset;
	private Instant blockedUntil = EPOCH;

	RateLimitCounter(Duration interval, Duration blockDuration, Clock clock) {
		this.interval = interval;
		this.blockDuration = blockDuration;
		this.clock = clock;
		this.lastReset = clock.instant();
	}

	public synchronized long increment() {
		resetIfNecessary();
		return ++count;
	}

	public synchronized boolean isBlocked() {
		return clock.instant().isBefore(blockedUntil);
	}

	public synchronized void block() {
		blockedUntil = clock.instant().plus(blockDuration);
		count = 0;
	}

	private void resetIfNecessary() {
		if (between(lastReset, clock.instant()).compareTo(interval) >= 0) {
			count = 0;
			lastReset = clock.instant();
		}
	}
}
