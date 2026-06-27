package dev.schoenberg.evergore.protocolParser.rest.filter;

import java.time.Clock;
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

@Singleton
@Filter("/**")
public class BrowserLoggingFilter implements HttpServerFilter {
	private final RateLimitConfiguration rateLimitConfiguration;
	private final Clock clock;
	private final Logger logger;
	private final Map<String, RateLimitCounter> counters = new ConcurrentHashMap<>();

	public BrowserLoggingFilter(RateLimitConfiguration rateLimitConfiguration, Clock clock, Logger logger) {
		this.rateLimitConfiguration = rateLimitConfiguration;
		this.clock = clock;
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

		RateLimitCounter counter = counters.computeIfAbsent(clientIp, k -> new RateLimitCounter(rateLimitConfiguration.interval(), rateLimitConfiguration.blockDuration(), clock));

		if (counter.increment() > rateLimitConfiguration.maxRequestsPerInterval()) {
			logger.info("Blocked:" + clientIp);
			counter.block();
		}

		if (counter.isBlocked()) {
			throw new TooManyRequests(clientIp);
		}

		return chain.proceed(request);
	}
}
