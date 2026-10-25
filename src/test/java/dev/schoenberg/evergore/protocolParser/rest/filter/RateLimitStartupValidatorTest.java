package dev.schoenberg.evergore.protocolParser.rest.filter;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import dev.schoenberg.evergore.protocolParser.LoggerSpy;
import dev.schoenberg.evergore.protocolParser.helper.config.RateLimitConfiguration;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RateLimitStartupValidatorTest {
	private static final long THIRTY_REQUESTS = 30;
	private static final Duration TEN_SECONDS = Duration.ofSeconds(10);
	private static final Duration ONE_MINUTE = Duration.ofMinutes(1);
	private static final int TEN_THOUSAND_CLIENTS = 10000;
	private static final int TEN_REQUESTS_AT_ONCE = 10;

	@Test
	void aWorkingConfigurationAllowsStartup() {
		RateLimitStartupValidator tested = validatorWith(THIRTY_REQUESTS, TEN_SECONDS, ONE_MINUTE, TEN_THOUSAND_CLIENTS);

		assertThatNoException().isThrownBy(tested::validateRateLimit);
	}

	@Test
	void aBudgetOfNoRequestsCausesStartupFailure() {
		RateLimitStartupValidator tested = validatorWith(0, TEN_SECONDS, ONE_MINUTE, TEN_THOUSAND_CLIENTS);

		assertStartupFailsNaming(tested, "evergore.rate-limit.max-requests-per-interval");
	}

	@Test
	void aZeroIntervalCausesStartupFailure() {
		RateLimitStartupValidator tested = validatorWith(THIRTY_REQUESTS, Duration.ZERO, ONE_MINUTE, TEN_THOUSAND_CLIENTS);

		assertStartupFailsNaming(tested, "evergore.rate-limit.interval");
	}

	@Test
	void aMissingIntervalCausesStartupFailure() {
		RateLimitStartupValidator tested = validatorWith(THIRTY_REQUESTS, null, ONE_MINUTE, TEN_THOUSAND_CLIENTS);

		assertStartupFailsNaming(tested, "evergore.rate-limit.interval");
	}

	@Test
	void aZeroBlockDurationCausesStartupFailure() {
		RateLimitStartupValidator tested = validatorWith(THIRTY_REQUESTS, TEN_SECONDS, Duration.ZERO, TEN_THOUSAND_CLIENTS);

		assertStartupFailsNaming(tested, "evergore.rate-limit.block-duration");
	}

	@Test
	void aNegativeBlockDurationCausesStartupFailure() {
		RateLimitStartupValidator tested = validatorWith(THIRTY_REQUESTS, TEN_SECONDS, ONE_MINUTE.negated(), TEN_THOUSAND_CLIENTS);

		assertStartupFailsNaming(tested, "evergore.rate-limit.block-duration");
	}

	@Test
	void noRoomForASingleClientCausesStartupFailure() {
		RateLimitStartupValidator tested = validatorWith(THIRTY_REQUESTS, TEN_SECONDS, ONE_MINUTE, 0);

		assertStartupFailsNaming(tested, "evergore.rate-limit.max-tracked-clients");
	}

	@Test
	void aNegativeClientBudgetCausesStartupFailure() {
		RateLimitStartupValidator tested = validatorWith(THIRTY_REQUESTS, TEN_SECONDS, ONE_MINUTE, -1);

		assertStartupFailsNaming(tested, "evergore.rate-limit.max-tracked-clients");
	}

	@Test
	void onApplicationEventPropagatesTheFailure() {
		RateLimitStartupValidator tested = validatorWith(THIRTY_REQUESTS, TEN_SECONDS, ONE_MINUTE, 0);

		assertThatThrownBy(() -> tested.onApplicationEvent(null)).isInstanceOf(IllegalStateException.class).hasMessageContaining("evergore.rate-limit.max-tracked-clients");
	}

	@Test
	void aBudgetOfNoRequestsNamesTheRequestsWithALoginThatWouldNeverBeAdmitted() {
		RateLimitStartupValidator tested = validatorWith(0, TEN_SECONDS, ONE_MINUTE, TEN_THOUSAND_CLIENTS);

		assertThatThrownBy(tested::validateRateLimit).hasMessageContaining("must be at least 1, otherwise no request with a login would ever be admitted");
	}

	@Test
	void anAnonymousBurstOfNoRequestsCausesStartupFailure() {
		RateLimitStartupValidator tested = validatorWith(THIRTY_REQUESTS, TEN_SECONDS, ONE_MINUTE, TEN_THOUSAND_CLIENTS, 0, TEN_SECONDS);

		assertThatThrownBy(tested::validateRateLimit)
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("Configuration property 'evergore.rate-limit.anonymous-burst' must be at least 1, otherwise no request without a login would ever be admitted.");
	}

	@Test
	void aNegativeAnonymousBurstCausesStartupFailure() {
		RateLimitStartupValidator tested = validatorWith(THIRTY_REQUESTS, TEN_SECONDS, ONE_MINUTE, TEN_THOUSAND_CLIENTS, -1, TEN_SECONDS);

		assertStartupFailsNaming(tested, "evergore.rate-limit.anonymous-burst");
	}

	@Test
	void aZeroAnonymousRefillIntervalCausesStartupFailure() {
		RateLimitStartupValidator tested = validatorWith(THIRTY_REQUESTS, TEN_SECONDS, ONE_MINUTE, TEN_THOUSAND_CLIENTS, TEN_REQUESTS_AT_ONCE, Duration.ZERO);

		assertThatThrownBy(tested::validateRateLimit)
				.isInstanceOf(IllegalStateException.class)
				.hasMessage(
						"Configuration property 'evergore.rate-limit.anonymous-refill-interval' must be a positive duration, otherwise a client without a login would never be turned away.");
	}

	@Test
	void aMissingAnonymousRefillIntervalCausesStartupFailure() {
		RateLimitStartupValidator tested = validatorWith(THIRTY_REQUESTS, TEN_SECONDS, ONE_MINUTE, TEN_THOUSAND_CLIENTS, TEN_REQUESTS_AT_ONCE, null);

		assertStartupFailsNaming(tested, "evergore.rate-limit.anonymous-refill-interval");
	}

	@Test
	void theSmallestAnonymousSettingsAllowStartup() {
		RateLimitStartupValidator tested = validatorWith(THIRTY_REQUESTS, TEN_SECONDS, ONE_MINUTE, TEN_THOUSAND_CLIENTS, 1, Duration.ofSeconds(1));

		assertThatNoException().isThrownBy(tested::validateRateLimit);
	}

	private void assertStartupFailsNaming(RateLimitStartupValidator tested, String property) {
		assertThatThrownBy(tested::validateRateLimit).isInstanceOf(IllegalStateException.class).hasMessageContaining(property);
	}

	private RateLimitStartupValidator validatorWith(long maxRequestsPerInterval, Duration interval, Duration blockDuration, int maxTrackedClients) {
		return validatorWith(maxRequestsPerInterval, interval, blockDuration, maxTrackedClients, TEN_REQUESTS_AT_ONCE, TEN_SECONDS);
	}

	private RateLimitStartupValidator validatorWith(long maxRequestsPerInterval, Duration interval, Duration blockDuration, int maxTrackedClients, int anonymousBurst,
			Duration anonymousRefillInterval) {
		RateLimitConfiguration configuration = new RateLimitConfiguration(maxRequestsPerInterval, interval, blockDuration, maxTrackedClients, anonymousBurst,
				anonymousRefillInterval);

		return new RateLimitStartupValidator(configuration, new LoggerSpy());
	}
}
