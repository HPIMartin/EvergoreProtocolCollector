package dev.schoenberg.evergore.protocolParser.rest.filter;

import static java.time.Duration.*;
import static java.time.Instant.*;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;

import org.reactivestreams.*;

import dev.schoenberg.evergore.protocolParser.*;
import dev.schoenberg.evergore.protocolParser.exceptions.*;
import io.micronaut.http.*;
import io.micronaut.http.annotation.*;
import io.micronaut.http.filter.*;
import jakarta.inject.*;

@Singleton
@Filter("/**")
public class BrowserLoggingFilter implements HttpServerFilter {
	private final Logger logger;

	private static final long MAX_REQUESTS_PER_INTERVAL = 5;
	private static final Duration RATE_LIMIT_INTERVAL = ofSeconds(10);
	private static final Duration BLOCK_INTERVAL = ofMinutes(1);

	private final Map<String, RateLimitCounter> counters;

	public BrowserLoggingFilter(Logger logger) {
		this.logger = logger;
		counters = new ConcurrentHashMap<>();
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

		RateLimitCounter counter = counters.computeIfAbsent(clientIp, k -> new RateLimitCounter());

		if (counter.increment() > MAX_REQUESTS_PER_INTERVAL) {
			logger.info("Blocked:" + clientIp);
			counter.block();
		}

		if (counter.isBlocked()) {
			throw new TooManyRequests();
		}


		return chain.proceed(request);
	}

	private static class RateLimitCounter {
		private volatile long count = 0;
		private volatile Instant lastReset = now();
		private volatile Instant blockedUntil = EPOCH;

		public synchronized long increment() {
			resetIfNecessary();
			return ++count;
		}

		public boolean isBlocked() {
			return now().isBefore(blockedUntil);
		}

		public void block() {
			blockedUntil = now().plus(BLOCK_INTERVAL);
			count = 0;
		}

		private void resetIfNecessary() {
			if (between(lastReset, now()).compareTo(RATE_LIMIT_INTERVAL) >= 0) {
				count = 0;
				lastReset = now();
			}
		}
	}
}
