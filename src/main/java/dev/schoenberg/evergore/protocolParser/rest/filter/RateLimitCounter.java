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

	synchronized long increment() {
		resetIfNecessary();
		return ++count;
	}

	synchronized boolean isBlocked() {
		return clock.instant().isBefore(blockedUntil);
	}

	synchronized void block() {
		blockedUntil = clock.instant().plus(blockDuration);
		count = 0;
	}

	synchronized boolean isIdle() {
		return !isBlocked() && intervalElapsed();
	}

	private void resetIfNecessary() {
		if (intervalElapsed()) {
			count = 0;
			lastReset = clock.instant();
		}
	}

	private boolean intervalElapsed() {
		return between(lastReset, clock.instant()).compareTo(interval) >= 0;
	}
}
