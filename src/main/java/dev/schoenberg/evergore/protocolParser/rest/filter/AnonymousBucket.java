package dev.schoenberg.evergore.protocolParser.rest.filter;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import static java.time.Duration.between;

class AnonymousBucket {
	private final int burst;
	private final Duration refillInterval;
	private final Clock clock;
	private long requestsLeft;
	private Instant lastRefill;

	AnonymousBucket(int burst, Duration refillInterval, Clock clock) {
		this.burst = burst;
		this.refillInterval = refillInterval;
		this.clock = clock;
		this.requestsLeft = burst;
		this.lastRefill = clock.instant();
	}

	synchronized boolean admits() {
		refill();
		if (requestsLeft <= 0) {
			return false;
		}

		requestsLeft--;
		return true;
	}

	synchronized boolean isFull() {
		refill();
		return requestsLeft == burst;
	}

	private void refill() {
		long refillsSinceLast = Math.max(0, between(lastRefill, clock.instant()).dividedBy(refillInterval));
		if (refillsSinceLast == 0) {
			return;
		}

		requestsLeft = Math.min(burst, requestsLeft + refillsSinceLast);
		lastRefill = lastRefill.plus(refillInterval.multipliedBy(refillsSinceLast));
	}
}
