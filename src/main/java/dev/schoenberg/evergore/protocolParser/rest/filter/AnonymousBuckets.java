package dev.schoenberg.evergore.protocolParser.rest.filter;

import java.time.Clock;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.inject.Singleton;

import dev.schoenberg.evergore.protocolParser.helper.config.RateLimitConfiguration;

@Singleton
class AnonymousBuckets {
	private static final int INITIAL_CAPACITY = 16;
	private static final float DEFAULT_LOAD_FACTOR = 0.75f;
	private static final boolean ACCESS_ORDER = true;

	private final RateLimitConfiguration configuration;
	private final Clock clock;
	private final Map<String, AnonymousBucket> buckets = new LinkedHashMap<>(INITIAL_CAPACITY, DEFAULT_LOAD_FACTOR, ACCESS_ORDER);

	AnonymousBuckets(RateLimitConfiguration configuration, Clock clock) {
		this.configuration = configuration;
		this.clock = clock;
	}

	synchronized boolean admits(String clientIp) {
		return trackedOrFresh(clientIp).admits();
	}

	synchronized List<String> trackedClients() {
		return List.copyOf(buckets.keySet());
	}

	private AnonymousBucket trackedOrFresh(String clientIp) {
		AnonymousBucket tracked = buckets.get(clientIp);
		if (tracked != null) {
			return tracked;
		}

		forgetFullBucketsFromTheLeastRecentlyUsedOn();
		AnonymousBucket fresh = new AnonymousBucket(configuration.anonymousBurst(), configuration.anonymousRefillInterval(), clock);
		buckets.put(clientIp, fresh);
		forgetLeastRecentlyUsedBucketWhenOverBudget();

		return fresh;
	}

	private void forgetFullBucketsFromTheLeastRecentlyUsedOn() {
		Iterator<AnonymousBucket> leastRecentlyUsedFirst = buckets.values().iterator();
		while (leastRecentlyUsedFirst.hasNext() && leastRecentlyUsedFirst.next().isFull()) {
			leastRecentlyUsedFirst.remove();
		}
	}

	private void forgetLeastRecentlyUsedBucketWhenOverBudget() {
		if (buckets.size() <= configuration.maxTrackedClients()) {
			return;
		}

		Iterator<String> leastRecentlyUsedFirst = buckets.keySet().iterator();
		leastRecentlyUsedFirst.next();
		leastRecentlyUsedFirst.remove();
	}
}
