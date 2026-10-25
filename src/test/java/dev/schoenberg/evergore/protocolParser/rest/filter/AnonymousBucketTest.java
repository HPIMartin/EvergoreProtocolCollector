package dev.schoenberg.evergore.protocolParser.rest.filter;

import java.time.Duration;
import java.time.Instant;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AnonymousBucketTest {
	private static final int BURST = 10;
	private static final Duration REFILL_INTERVAL = Duration.ofSeconds(10);
	private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");

	private final MutableClock clock = new MutableClock(START);
	private final AnonymousBucket tested = new AnonymousBucket(BURST, REFILL_INTERVAL, clock);

	@Test
	void admitsTheBurstAtOnce() {
		boolean lastOfTheBurstAdmitted = spend(BURST);

		assertThat(lastOfTheBurstAdmitted).isTrue();
	}

	@Test
	void turnsAwayTheRequestBeyondTheBurst() {
		spend(BURST);

		boolean admitted = tested.admits();

		assertThat(admitted).isFalse();
	}

	@Test
	void turnsAwayTheRequestOneSecondBeforeTheRefillInterval() {
		spend(BURST);
		clock.advanceBy(REFILL_INTERVAL.minusSeconds(1));

		boolean admitted = tested.admits();

		assertThat(admitted).isFalse();
	}

	@Test
	void admitsOneRequestOnceTheRefillIntervalIsOver() {
		spend(BURST);
		clock.advanceBy(REFILL_INTERVAL);

		boolean admitted = tested.admits();

		assertThat(admitted).isTrue();
	}

	@Test
	void admitsOnlyOneRequestPerRefillInterval() {
		spend(BURST);
		clock.advanceBy(REFILL_INTERVAL);
		tested.admits();

		boolean admitted = tested.admits();

		assertThat(admitted).isFalse();
	}

	@Test
	void countsTheRefillIntervalFromTheFirstRequestNotFromTheLastOne() {
		spend(BURST);
		clock.advanceBy(Duration.ofSeconds(6));
		tested.admits();
		clock.advanceBy(Duration.ofSeconds(4));

		boolean admitted = tested.admits();

		assertThat(admitted).isTrue();
	}

	@Test
	void savesUpNoMoreThanTheBurstDuringALongPause() {
		spend(BURST);
		clock.advanceBy(Duration.ofMinutes(5));
		spend(BURST);

		boolean admitted = tested.admits();

		assertThat(admitted).isFalse();
	}

	@Test
	void isFullWhileNothingWasSpent() {
		boolean full = tested.isFull();

		assertThat(full).isTrue();
	}

	@Test
	void isNotFullAfterARequest() {
		tested.admits();

		boolean full = tested.isFull();

		assertThat(full).isFalse();
	}

	@Test
	void isFullAgainOnceTheRefillIntervalGaveBackWhatWasSpent() {
		tested.admits();
		clock.advanceBy(REFILL_INTERVAL);

		boolean full = tested.isFull();

		assertThat(full).isTrue();
	}

	@Test
	void turnsEveryRequestAwayWhenTheClockStepsBackWhileTheBucketIsEmpty() {
		spend(BURST);
		clock.advanceBy(Duration.ofSeconds(-60));

		long admitted = IntStream.range(0, 1000).filter(request -> tested.admits()).count();

		assertThat(admitted).isZero();
	}

	@Test
	void keepsTheBucketShutUntilTheClockHasCaughtUpWithTheSteppedBackMoment() {
		spend(BURST);
		clock.advanceBy(Duration.ofSeconds(-60));
		clock.advanceBy(Duration.ofSeconds(60).plus(REFILL_INTERVAL).minusSeconds(1));
		boolean admittedBeforeTheInterval = tested.admits();
		clock.advanceBy(Duration.ofSeconds(1));

		boolean admittedAfterTheInterval = tested.admits();

		assertThat(admittedBeforeTheInterval).isFalse();
		assertThat(admittedAfterTheInterval).isTrue();
	}

	@Test
	void doesNotCallABucketFullWhenTheClockSteppedBackWhileItWasLow() {
		spend(BURST);
		clock.advanceBy(Duration.ofSeconds(-600));

		boolean full = tested.isFull();

		assertThat(full).isFalse();
	}

	private boolean spend(int requests) {
		boolean admitted = false;
		for (int request = 0; request < requests; request++) {
			admitted = tested.admits();
		}
		return admitted;
	}
}
