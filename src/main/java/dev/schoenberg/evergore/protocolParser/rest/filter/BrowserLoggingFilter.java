package dev.schoenberg.evergore.protocolParser.rest.filter;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.inject.Singleton;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.HttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import org.reactivestreams.Publisher;

import dev.schoenberg.evergore.protocolParser.Logger;
import dev.schoenberg.evergore.protocolParser.exceptions.TooManyRequests;
import dev.schoenberg.evergore.protocolParser.helper.config.RateLimitConfiguration;

import static java.time.Duration.between;
import static java.time.Instant.EPOCH;
import static java.time.Instant.now;

@Singleton
@Filter("/**")
public class BrowserLoggingFilter implements HttpServerFilter {
	private final RateLimitConfiguration rateLimitConfiguration;
	private final Logger logger;
	private final Map<String, RateLimitCounter> counters = new ConcurrentHashMap<>();

	public BrowserLoggingFilter(RateLimitConfiguration rateLimitConfiguration, Logger logger) {
		this.rateLimitConfiguration = rateLimitConfiguration;
		this.logger = logger;
	}

	@Override
	public int getOrder() {
		return 1;
	}

	@Override
	public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {
		String userAgent = request.getHeaders().get("user-agent");
		String clientIp = request.getRemoteAddress().getAddress().getHostAddress();
		logger.info("Client IP: " + clientIp + " Agent: " + userAgent);

		RateLimitCounter counter = counters.computeIfAbsent(clientIp, k -> new RateLimitCounter(rateLimitConfiguration.interval(), rateLimitConfiguration.blockDuration()));

		if (counter.increment() > rateLimitConfiguration.maxRequestsPerInterval()) {
			logger.info("Blocked:" + clientIp);
			counter.block();
		}

		if (counter.isBlocked()) {
			throw new TooManyRequests(clientIp);
		}

		return chain.proceed(request);
	}

	private static class RateLimitCounter {
		private final Duration interval;
		private final Duration blockDuration;
		private volatile long count = 0;
		private volatile Instant lastReset = now();
		private volatile Instant blockedUntil = EPOCH;

		RateLimitCounter(Duration interval, Duration blockDuration) {
			this.interval = interval;
			this.blockDuration = blockDuration;
		}

		public synchronized long increment() {
			resetIfNecessary();
			return ++count;
		}

		public boolean isBlocked() {
			return now().isBefore(blockedUntil);
		}

		public void block() {
			blockedUntil = now().plus(blockDuration);
			count = 0;
		}

		private void resetIfNecessary() {
			if (between(lastReset, now()).compareTo(interval) >= 0) {
				count = 0;
				lastReset = now();
			}
		}
	}
}
