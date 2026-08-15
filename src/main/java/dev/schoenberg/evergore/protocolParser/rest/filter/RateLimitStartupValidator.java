package dev.schoenberg.evergore.protocolParser.rest.filter;

import java.time.Duration;

import jakarta.inject.Singleton;

import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.context.event.StartupEvent;

import dev.schoenberg.evergore.protocolParser.Logger;
import dev.schoenberg.evergore.protocolParser.helper.config.RateLimitConfiguration;

@Singleton
public class RateLimitStartupValidator implements ApplicationEventListener<StartupEvent> {
	private static final String MAX_REQUESTS_PER_INTERVAL = "evergore.rate-limit.max-requests-per-interval";
	private static final String INTERVAL = "evergore.rate-limit.interval";
	private static final String BLOCK_DURATION = "evergore.rate-limit.block-duration";
	private static final String MAX_TRACKED_CLIENTS = "evergore.rate-limit.max-tracked-clients";

	private final RateLimitConfiguration rateLimitConfiguration;
	private final Logger logger;

	public RateLimitStartupValidator(RateLimitConfiguration rateLimitConfiguration, Logger logger) {
		this.rateLimitConfiguration = rateLimitConfiguration;
		this.logger = logger;
	}

	@Override
	public void onApplicationEvent(StartupEvent event) {
		validateRateLimit();
	}

	void validateRateLimit() {
		requireAtLeastOne(rateLimitConfiguration.maxRequestsPerInterval(), MAX_REQUESTS_PER_INTERVAL, "no request would ever be admitted");
		requirePositive(rateLimitConfiguration.interval(), INTERVAL, "every request would start a fresh count");
		requirePositive(rateLimitConfiguration.blockDuration(), BLOCK_DURATION, "a blocked client would be admitted again right away");
		requireAtLeastOne(rateLimitConfiguration.maxTrackedClients(), MAX_TRACKED_CLIENTS, "no client could be counted");
	}

	private void requireAtLeastOne(long configured, String property, String consequence) {
		if (configured < 1) {
			reject(property, "must be at least 1, otherwise " + consequence);
		}
	}

	private void requirePositive(Duration configured, String property, String consequence) {
		if (configured == null || configured.isZero() || configured.isNegative()) {
			reject(property, "must be a positive duration, otherwise " + consequence);
		}
	}

	private void reject(String property, String requirement) {
		String reason = "Configuration property '" + property + "' " + requirement + ".";
		logger.error(reason);
		throw new IllegalStateException(reason);
	}
}
