package dev.schoenberg.evergore.protocolParser.rest.filter;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import dev.schoenberg.evergore.protocolParser.LoggerSpy;
import dev.schoenberg.evergore.protocolParser.helper.config.RateLimitConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitCountersTest {
	private static final Duration INTERVAL = Duration.ofSeconds(10);
	private static final Duration BLOCK_DURATION = Duration.ofMinutes(1);
	private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");
	private static final long ONE_REQUEST_PER_INTERVAL = 1;
	private static final int TWO_CLIENTS = 2;
	private static final int ONE_CLIENT = 1;
	private static final int MANY_CLIENTS = 100;
	private static final String FIRST_CLIENT = "10.0.0.1";
	private static final String SECOND_CLIENT = "10.0.0.2";
	private static final String THIRD_CLIENT = "10.0.0.3";

	private final MutableClock clock = new MutableClock(START);

	@Test
	void admitsARequestWithinTheBudget() {
		RateLimitCounters tested = countersFor(TWO_CLIENTS);

		boolean blocked = tested.blocks(FIRST_CLIENT);

		assertThat(blocked).isFalse();
	}

	@Test
	void blocksTheRequestThatExceedsTheBudget() {
		RateLimitCounters tested = countersFor(TWO_CLIENTS);
		tested.blocks(FIRST_CLIENT);

		boolean blocked = tested.blocks(FIRST_CLIENT);

		assertThat(blocked).as("a known client keeps its counter, otherwise its budget resets per request").isTrue();
	}

	@Test
	void countsEachClientAgainstItsOwnBudget() {
		RateLimitCounters tested = countersFor(TWO_CLIENTS);
		tested.blocks(FIRST_CLIENT);

		boolean blocked = tested.blocks(SECOND_CLIENT);

		assertThat(blocked).isFalse();
	}

	@Test
	void admitsAClientAgainOnceItsBlockExpired() {
		RateLimitCounters tested = countersFor(TWO_CLIENTS);
		blockByExceedingTheBudget(tested, FIRST_CLIENT);
		clock.advanceBy(BLOCK_DURATION.plusSeconds(1));

		boolean blocked = tested.blocks(FIRST_CLIENT);

		assertThat(blocked).isFalse();
	}

	@Test
	void tracksEveryClientItCounted() {
		RateLimitCounters tested = countersFor(TWO_CLIENTS);
		tested.blocks(FIRST_CLIENT);

		tested.blocks(SECOND_CLIENT);

		List<String> tracked = tested.trackedClients();

		assertThat(tracked).containsExactly(FIRST_CLIENT, SECOND_CLIENT);
	}

	@Test
	void forgetsAClientWhoseIntervalElapsedWhenANewClientAppears() {
		RateLimitCounters tested = countersFor(TWO_CLIENTS);
		tested.blocks(FIRST_CLIENT);
		clock.advanceBy(INTERVAL);

		tested.blocks(SECOND_CLIENT);

		List<String> tracked = tested.trackedClients();

		assertThat(tracked).as("an elapsed interval means the counter would reset anyway").containsExactly(SECOND_CLIENT);
	}

	@Test
	void keepsABlockedClientWhoseIntervalElapsed() {
		RateLimitCounters tested = countersFor(TWO_CLIENTS);
		blockByExceedingTheBudget(tested, FIRST_CLIENT);
		clock.advanceBy(INTERVAL);

		tested.blocks(SECOND_CLIENT);

		List<String> tracked = tested.trackedClients();

		assertThat(tracked).as("forgetting a blocked client would hand it a fresh budget").containsExactly(FIRST_CLIENT, SECOND_CLIENT);
	}

	@Test
	void forgetsTheLeastRecentlyUsedClientWhenTheBudgetIsFull() {
		RateLimitCounters tested = countersFor(TWO_CLIENTS);
		tested.blocks(FIRST_CLIENT);
		tested.blocks(SECOND_CLIENT);

		tested.blocks(THIRD_CLIENT);

		List<String> tracked = tested.trackedClients();

		assertThat(tracked).containsExactly(SECOND_CLIENT, THIRD_CLIENT);
	}

	@Test
	void countsAKnownClientAsRecentlyUsedAgain() {
		RateLimitCounters tested = countersFor(TWO_CLIENTS);
		tested.blocks(FIRST_CLIENT);
		tested.blocks(SECOND_CLIENT);
		tested.blocks(FIRST_CLIENT);

		tested.blocks(THIRD_CLIENT);

		List<String> tracked = tested.trackedClients();

		assertThat(tracked).as("the second client became the least recently used one").containsExactly(FIRST_CLIENT, THIRD_CLIENT);
	}

	@Test
	void forgetsEvenABlockedClientWhenItIsTheOnlyWayToStayInBudget() {
		RateLimitCounters tested = countersFor(ONE_CLIENT);
		blockByExceedingTheBudget(tested, FIRST_CLIENT);

		tested.blocks(SECOND_CLIENT);

		List<String> tracked = tested.trackedClients();

		assertThat(tracked).as("the configured bound wins over a running block").containsExactly(SECOND_CLIENT);
	}

	@Test
	void staysWithinTheBudgetWhileManyClientsAppear() {
		RateLimitCounters tested = countersFor(TWO_CLIENTS);

		for (int client = 0; client < MANY_CLIENTS; client++) {
			tested.blocks("10.0.1." + client);
		}

		List<String> tracked = tested.trackedClients();

		assertThat(tracked).hasSize(TWO_CLIENTS);
	}

	@Test
	void logsTheClientItBlocks() {
		LoggerSpy logger = new LoggerSpy();
		RateLimitCounters tested = countersFor(TWO_CLIENTS, logger);
		tested.blocks(FIRST_CLIENT);

		tested.blocks(FIRST_CLIENT);

		assertThat(logger.infoMessages()).containsExactly("Blocked:" + FIRST_CLIENT);
	}

	private void blockByExceedingTheBudget(RateLimitCounters counters, String clientIp) {
		counters.blocks(clientIp);
		counters.blocks(clientIp);
	}

	private RateLimitCounters countersFor(int maxTrackedClients) {
		return countersFor(maxTrackedClients, new LoggerSpy());
	}

	private RateLimitCounters countersFor(int maxTrackedClients, LoggerSpy logger) {
		return new RateLimitCounters(new RateLimitConfiguration(ONE_REQUEST_PER_INTERVAL, INTERVAL, BLOCK_DURATION, maxTrackedClients), clock, logger);
	}
}
