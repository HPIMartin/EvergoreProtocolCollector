package dev.schoenberg.evergore.protocolParser.rest.filter;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import dev.schoenberg.evergore.protocolParser.LoggerSpy;
import dev.schoenberg.evergore.protocolParser.application.LoginGenerations;
import dev.schoenberg.evergore.protocolParser.application.ValidLogins;
import dev.schoenberg.evergore.protocolParser.helper.config.RateLimitConfiguration;
import dev.schoenberg.evergore.protocolParser.helper.config.SecurityConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

class FilterChainOrderTest {
	private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");
	private static final Duration INTERVAL = Duration.ofSeconds(10);
	private static final Duration BLOCK_DURATION = Duration.ofMinutes(1);
	private static final long ONE_REQUEST_PER_INTERVAL = 1;
	private static final int ONE_CLIENT = 1;

	private final ClientIp clientIp = new ClientIp();
	private final LoggerSpy logger = new LoggerSpy();
	private final PathCanonicalizer canonicalizer = new PathCanonicalizer();

	@Test
	void logsThenReadsTheLoginThenThrottlesThenAnswersAnUnreadableRequestThenChecksTheAccessThenLimitsTheLoginBody() {
		List<Integer> orders = List
				.of(auditLogFilter().getOrder(), loginReadingFilter().getOrder(), rateLimitFilter().getOrder(), new UnreadableRequestFilter().getOrder(), accessFilter().getOrder(),
						new LoginBodyLimitFilter(canonicalizer).getOrder());

		assertThat(orders).containsExactly(1, 2, 3, 4, 5, 6);
	}

	private RequestAuditLogFilter auditLogFilter() {
		return new RequestAuditLogFilter(clientIp, logger);
	}

	private LoginReadingFilter loginReadingFilter() {
		return new LoginReadingFilter(new ValidLogins(new NoSignedLogins(), new LoginGenerations()));
	}

	private RateLimitFilter rateLimitFilter() {
		RateLimitConfiguration configuration = new RateLimitConfiguration(ONE_REQUEST_PER_INTERVAL, INTERVAL, BLOCK_DURATION, ONE_CLIENT);

		return new RateLimitFilter(new RateLimitCounters(configuration, new MutableClock(START), logger), clientIp);
	}

	private AccessFilter accessFilter() {
		SecurityConfiguration configuration = new SecurityConfiguration(List.of());

		return new AccessFilter(canonicalizer, new PublicPaths(configuration), new SpaNavigationPaths(), logger);
	}
}
