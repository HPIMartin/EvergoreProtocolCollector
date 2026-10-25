package dev.schoenberg.evergore.protocolParser.rest.filter;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import dev.schoenberg.evergore.protocolParser.helper.config.RateLimitConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

class AnonymousBucketsTest {
	private static final Duration REFILL_INTERVAL = Duration.ofSeconds(10);
	private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");
	private static final int ONE_REQUEST_AT_ONCE = 1;
	private static final int TWO_CLIENTS = 2;
	private static final int MANY_CLIENTS = 100;
	private static final int FEW_CLOCK_READS = 5;
	private static final String FIRST_CLIENT = "10.0.0.1";
	private static final String SECOND_CLIENT = "10.0.0.2";
	private static final String THIRD_CLIENT = "10.0.0.3";

	private final MutableClock clock = new MutableClock(START);

	@Test
	void admitsTheFirstRequestOfAnAddress() {
		AnonymousBuckets tested = bucketsFor(TWO_CLIENTS);

		boolean admitted = tested.admits(FIRST_CLIENT);

		assertThat(admitted).isTrue();
	}

	@Test
	void turnsAnAddressAwayOnceItsBucketIsEmpty() {
		AnonymousBuckets tested = bucketsFor(TWO_CLIENTS);
		tested.admits(FIRST_CLIENT);

		boolean admitted = tested.admits(FIRST_CLIENT);

		assertThat(admitted).isFalse();
	}

	@Test
	void countsEachAddressInABucketOfItsOwn() {
		AnonymousBuckets tested = bucketsFor(TWO_CLIENTS);
		tested.admits(FIRST_CLIENT);

		boolean admitted = tested.admits(SECOND_CLIENT);

		assertThat(admitted).isTrue();
	}

	@Test
	void tracksEveryAddressItCounted() {
		AnonymousBuckets tested = bucketsFor(TWO_CLIENTS);
		tested.admits(FIRST_CLIENT);
		tested.admits(SECOND_CLIENT);

		List<String> tracked = tested.trackedClients();

		assertThat(tracked).containsExactly(FIRST_CLIENT, SECOND_CLIENT);
	}

	@Test
	void forgetsAnAddressWhoseBucketIsFullAgainWhenANewAddressAppears() {
		AnonymousBuckets tested = bucketsFor(TWO_CLIENTS);
		tested.admits(FIRST_CLIENT);
		clock.advanceBy(REFILL_INTERVAL);

		tested.admits(SECOND_CLIENT);

		List<String> tracked = tested.trackedClients();

		assertThat(tracked).containsExactly(SECOND_CLIENT);
	}

	@Test
	void keepsAnAddressWhoseBucketIsStillEmptyWhenANewAddressAppears() {
		AnonymousBuckets tested = bucketsFor(TWO_CLIENTS);
		tested.admits(FIRST_CLIENT);

		tested.admits(SECOND_CLIENT);

		List<String> tracked = tested.trackedClients();

		assertThat(tracked).containsExactly(FIRST_CLIENT, SECOND_CLIENT);
	}

	@Test
	void forgetsTheLeastRecentlyUsedAddressWhenTheBudgetIsFull() {
		AnonymousBuckets tested = bucketsFor(TWO_CLIENTS);
		tested.admits(FIRST_CLIENT);
		tested.admits(SECOND_CLIENT);
		tested.admits(FIRST_CLIENT);

		tested.admits(THIRD_CLIENT);

		List<String> tracked = tested.trackedClients();

		assertThat(tracked).containsExactly(FIRST_CLIENT, THIRD_CLIENT);
	}

	@Test
	void staysWithinTheBudgetWhileManyAddressesAppear() {
		AnonymousBuckets tested = bucketsFor(TWO_CLIENTS);

		for (int client = 0; client < MANY_CLIENTS; client++) {
			tested.admits("10.0.1." + client);
		}

		List<String> tracked = tested.trackedClients();

		assertThat(tracked).hasSize(TWO_CLIENTS);
	}

	@Test
	void readsTheClockAFewTimesForANewAddressWhateverNumberOfAddressesItTracks() {
		AnonymousBuckets tested = bucketsFor(MANY_CLIENTS);
		for (int client = 0; client < MANY_CLIENTS; client++) {
			tested.admits("10.0.2." + client);
		}
		int readsBefore = clock.reads();

		tested.admits(THIRD_CLIENT);

		assertThat(clock.reads() - readsBefore).isLessThanOrEqualTo(FEW_CLOCK_READS);
	}

	@Test
	void forgetsEveryAddressWhoseBucketIsFullAgainFromTheLeastRecentlyUsedOnwards() {
		AnonymousBuckets tested = bucketsFor(MANY_CLIENTS);
		tested.admits(FIRST_CLIENT);
		tested.admits(SECOND_CLIENT);
		clock.advanceBy(REFILL_INTERVAL);

		tested.admits(THIRD_CLIENT);

		assertThat(tested.trackedClients()).containsExactly(THIRD_CLIENT);
	}

	private AnonymousBuckets bucketsFor(int maxTrackedClients) {
		RateLimitConfiguration configuration = new RateLimitConfiguration(1, REFILL_INTERVAL, Duration.ofMinutes(1), maxTrackedClients, ONE_REQUEST_AT_ONCE, REFILL_INTERVAL);

		return new AnonymousBuckets(configuration, clock);
	}
}
