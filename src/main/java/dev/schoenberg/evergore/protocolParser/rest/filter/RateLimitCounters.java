package dev.schoenberg.evergore.protocolParser.rest.filter;

import java.time.Clock;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.inject.Singleton;

import dev.schoenberg.evergore.protocolParser.Logger;
import dev.schoenberg.evergore.protocolParser.helper.config.RateLimitConfiguration;

@Singleton
class RateLimitCounters {
	private static final int INITIAL_CAPACITY = 16;
	private static final float DEFAULT_LOAD_FACTOR = 0.75f;
	private static final boolean ACCESS_ORDER = true;

	private final RateLimitConfiguration configuration;
	private final Clock clock;
	private final Logger logger;
	private final Map<String, RateLimitCounter> counters = new LinkedHashMap<>(INITIAL_CAPACITY, DEFAULT_LOAD_FACTOR, ACCESS_ORDER);

	RateLimitCounters(RateLimitConfiguration configuration, Clock clock, Logger logger) {
		this.configuration = configuration;
		this.clock = clock;
		this.logger = logger;
	}

	synchronized boolean blocks(String clientIp) {
		RateLimitCounter counter = trackedOrFresh(clientIp);

		if (counter.increment() > configuration.maxRequestsPerInterval()) {
			logger.info("Blocked:" + clientIp);
			counter.block();
		}

		return counter.isBlocked();
	}

	synchronized List<String> trackedClients() {
		return List.copyOf(counters.keySet());
	}

	private RateLimitCounter trackedOrFresh(String clientIp) {
		RateLimitCounter tracked = counters.get(clientIp);
		if (tracked != null) {
			return tracked;
		}

		forgetIdleClients();
		RateLimitCounter fresh = new RateLimitCounter(configuration.interval(), configuration.blockDuration(), clock);
		counters.put(clientIp, fresh);
		forgetLeastRecentlyUsedClientWhenOverBudget();

		return fresh;
	}

	private void forgetIdleClients() {
		counters.values().removeIf(RateLimitCounter::isIdle);
	}

	private void forgetLeastRecentlyUsedClientWhenOverBudget() {
		if (counters.size() <= configuration.maxTrackedClients()) {
			return;
		}

		Iterator<String> leastRecentlyUsedFirst = counters.keySet().iterator();
		leastRecentlyUsedFirst.next();
		leastRecentlyUsedFirst.remove();
	}
}
