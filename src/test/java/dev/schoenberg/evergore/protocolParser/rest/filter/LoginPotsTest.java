package dev.schoenberg.evergore.protocolParser.rest.filter;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Test;

import dev.schoenberg.evergore.protocolParser.LoggerSpy;
import dev.schoenberg.evergore.protocolParser.helper.config.RateLimitConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

class LoginPotsTest {
	private static final long THREE_REQUESTS_PER_COUNT = 3;
	private static final Duration COUNT = Duration.ofSeconds(10);
	private static final Duration BLOCK_DURATION = Duration.ofMinutes(1);
	private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");
	private static final int MANY_ADDRESSES = 100;
	private static final int TWO_POTS = 2;
	private static final int TEN_AT_ONCE = 10;
	private static final String FIRST_ADDRESS = "10.0.0.1";
	private static final String SECOND_ADDRESS = "10.0.0.2";
	private static final String THIRD_ADDRESS = "10.0.0.3";
	private static final String AURORA = "Aurora";
	private static final String BOREAS = "Boreas";
	private static final String CALIX = "Calix";

	private final MutableClock clock = new MutableClock(START);

	@Test
	void admitsTheRequestsOfAPotUpToItsBudget() {
		LoginPots tested = potsHolding(TWO_POTS);

		boolean turnedAway = anyTurnedAway(tested, THREE_REQUESTS_PER_COUNT, FIRST_ADDRESS, AURORA);

		assertThat(turnedAway).isFalse();
	}

	@Test
	void turnsAwayTheRequestBeyondTheBudgetOfAPot() {
		LoginPots tested = potsHolding(TWO_POTS);
		anyTurnedAway(tested, THREE_REQUESTS_PER_COUNT, FIRST_ADDRESS, AURORA);

		boolean turnedAway = tested.turnsAway(FIRST_ADDRESS, AURORA);

		assertThat(turnedAway).isTrue();
	}

	@Test
	void startsAFreshCountOnceTheCountIsOver() {
		LoginPots tested = potsHolding(TWO_POTS);
		anyTurnedAway(tested, THREE_REQUESTS_PER_COUNT, FIRST_ADDRESS, AURORA);
		clock.advanceBy(COUNT);

		boolean turnedAway = tested.turnsAway(FIRST_ADDRESS, AURORA);

		assertThat(turnedAway).isFalse();
	}

	@Test
	void keepsTheCountOneSecondBeforeItIsOver() {
		LoginPots tested = potsHolding(TWO_POTS);
		anyTurnedAway(tested, THREE_REQUESTS_PER_COUNT, FIRST_ADDRESS, AURORA);
		clock.advanceBy(COUNT.minusSeconds(1));

		boolean turnedAway = tested.turnsAway(FIRST_ADDRESS, AURORA);

		assertThat(turnedAway).isTrue();
	}

	@Test
	void countsTheSameAccountFromAnotherAddressInTheSamePot() {
		LoginPots tested = potsHolding(TWO_POTS);
		anyTurnedAway(tested, THREE_REQUESTS_PER_COUNT, FIRST_ADDRESS, AURORA);

		boolean turnedAway = tested.turnsAway(SECOND_ADDRESS, AURORA);

		assertThat(turnedAway).isTrue();
	}

	@Test
	void countsAnotherAccountFromTheSameAddressInTheSamePot() {
		LoginPots tested = potsHolding(TWO_POTS);
		anyTurnedAway(tested, THREE_REQUESTS_PER_COUNT, FIRST_ADDRESS, AURORA);

		boolean turnedAway = tested.turnsAway(FIRST_ADDRESS, BOREAS);

		assertThat(turnedAway).isTrue();
	}

	@Test
	void countsAnotherAccountFromAnotherAddressInAPotOfItsOwn() {
		LoginPots tested = potsHolding(TWO_POTS);
		anyTurnedAway(tested, THREE_REQUESTS_PER_COUNT, FIRST_ADDRESS, AURORA);

		boolean turnedAway = tested.turnsAway(SECOND_ADDRESS, BOREAS);

		assertThat(turnedAway).isFalse();
	}

	@Test
	void countsTwoPotsTogetherOnceOneRequestLinksThem() {
		LoginPots tested = potsHolding(TWO_POTS);
		anyTurnedAway(tested, 2, FIRST_ADDRESS, AURORA);
		anyTurnedAway(tested, 1, SECOND_ADDRESS, BOREAS);

		boolean turnedAway = tested.turnsAway(SECOND_ADDRESS, AURORA);

		assertThat(turnedAway).isTrue();
	}

	@Test
	void countsTheRequestsOfTwoPotsTogetherAfterTheyWereLinked() {
		LoginPots tested = potsHolding(TWO_POTS);
		tested.turnsAway(FIRST_ADDRESS, AURORA);
		tested.turnsAway(SECOND_ADDRESS, BOREAS);
		tested.turnsAway(SECOND_ADDRESS, AURORA);

		boolean turnedAway = tested.turnsAway(FIRST_ADDRESS, BOREAS);

		assertThat(turnedAway).isTrue();
	}

	@Test
	void turnsAwayEveryAddressOfAPotForTheBlockDuration() {
		LoginPots tested = potsHolding(TWO_POTS);
		tested.turnsAway(SECOND_ADDRESS, AURORA);
		anyTurnedAway(tested, THREE_REQUESTS_PER_COUNT + 1, FIRST_ADDRESS, AURORA);
		clock.advanceBy(BLOCK_DURATION.minusSeconds(1));

		boolean turnedAway = tested.turnsAwayAddress(SECOND_ADDRESS);

		assertThat(turnedAway).isTrue();
	}

	@Test
	void turnsAwayEveryAccountOfAPotForTheBlockDuration() {
		LoginPots tested = potsHolding(TWO_POTS);
		tested.turnsAway(FIRST_ADDRESS, BOREAS);
		anyTurnedAway(tested, THREE_REQUESTS_PER_COUNT + 1, FIRST_ADDRESS, AURORA);
		clock.advanceBy(BLOCK_DURATION.minusSeconds(1));

		boolean turnedAway = tested.turnsAway(SECOND_ADDRESS, BOREAS);

		assertThat(turnedAway).isTrue();
	}

	@Test
	void turnsNoAddressAwayOnceTheBlockDurationIsOver() {
		LoginPots tested = potsHolding(TWO_POTS);
		anyTurnedAway(tested, THREE_REQUESTS_PER_COUNT + 1, FIRST_ADDRESS, AURORA);
		clock.advanceBy(BLOCK_DURATION);

		boolean turnedAway = tested.turnsAwayAddress(FIRST_ADDRESS);

		assertThat(turnedAway).isFalse();
	}

	@Test
	void turnsNoAddressAwayThatNoPotHolds() {
		LoginPots tested = potsHolding(TWO_POTS);
		anyTurnedAway(tested, THREE_REQUESTS_PER_COUNT + 1, FIRST_ADDRESS, AURORA);

		boolean turnedAway = tested.turnsAwayAddress(THIRD_ADDRESS);

		assertThat(turnedAway).isFalse();
	}

	@Test
	void turnsNoAddressAwayWhileItsPotIsWithinItsBudget() {
		LoginPots tested = potsHolding(TWO_POTS);
		anyTurnedAway(tested, THREE_REQUESTS_PER_COUNT, FIRST_ADDRESS, AURORA);

		boolean turnedAway = tested.turnsAwayAddress(FIRST_ADDRESS);

		assertThat(turnedAway).isFalse();
	}

	@Test
	void doesNotTurnAnAddressAwayLongerForAnotherRequestWhileItIsTurnedAway() {
		LoginPots tested = potsHolding(TWO_POTS);
		anyTurnedAway(tested, THREE_REQUESTS_PER_COUNT + 1, FIRST_ADDRESS, AURORA);
		clock.advanceBy(BLOCK_DURATION.minusSeconds(1));
		tested.turnsAway(FIRST_ADDRESS, AURORA);
		clock.advanceBy(Duration.ofSeconds(1));

		boolean turnedAway = tested.turnsAway(FIRST_ADDRESS, AURORA);

		assertThat(turnedAway).isFalse();
	}

	@Test
	void forgetsAPotWhoseCountIsOverSoItsAddressesAreCountedApart() {
		LoginPots tested = potsHolding(TWO_POTS);
		tested.turnsAway(FIRST_ADDRESS, AURORA);
		tested.turnsAway(SECOND_ADDRESS, AURORA);
		clock.advanceBy(COUNT);
		anyTurnedAway(tested, THREE_REQUESTS_PER_COUNT, FIRST_ADDRESS, BOREAS);
		clock.advanceBy(Duration.ofSeconds(1));

		boolean turnedAway = tested.turnsAway(SECOND_ADDRESS, CALIX);

		assertThat(turnedAway).isFalse();
	}

	@Test
	void forgetsATurnedAwayPotOnceItsBlockDurationIsOverSoItsAddressesAreCountedApart() {
		LoginPots tested = potsHolding(TWO_POTS);
		anyTurnedAway(tested, THREE_REQUESTS_PER_COUNT + 1, FIRST_ADDRESS, AURORA);
		clock.advanceBy(BLOCK_DURATION);
		anyTurnedAway(tested, THREE_REQUESTS_PER_COUNT, SECOND_ADDRESS, AURORA);

		boolean turnedAway = tested.turnsAway(FIRST_ADDRESS, BOREAS);

		assertThat(turnedAway).isFalse();
	}

	@Test
	void keepsATurnedAwayPotWhoseCountIsOver() {
		LoginPots tested = potsHolding(TWO_POTS);
		anyTurnedAway(tested, THREE_REQUESTS_PER_COUNT + 1, FIRST_ADDRESS, AURORA);
		clock.advanceBy(COUNT);

		boolean turnedAway = tested.turnsAway(SECOND_ADDRESS, AURORA);

		assertThat(turnedAway).isTrue();
	}

	@Test
	void tracksTheAddressesOfEveryPot() {
		LoginPots tested = potsHolding(TWO_POTS);
		tested.turnsAway(FIRST_ADDRESS, AURORA);
		tested.turnsAway(SECOND_ADDRESS, BOREAS);

		List<String> tracked = tested.trackedAddresses();

		assertThat(tracked).containsExactlyInAnyOrder(FIRST_ADDRESS, SECOND_ADDRESS);
	}

	@Test
	void forgetsTheLeastRecentlyUsedPotWhenThereAreMoreThanTheBudgetAllows() {
		LoginPots tested = potsHolding(TWO_POTS);
		tested.turnsAway(FIRST_ADDRESS, AURORA);
		tested.turnsAway(SECOND_ADDRESS, BOREAS);
		tested.turnsAway(FIRST_ADDRESS, AURORA);

		tested.turnsAway(THIRD_ADDRESS, CALIX);

		List<String> tracked = tested.trackedAddresses();

		assertThat(tracked).containsExactlyInAnyOrder(FIRST_ADDRESS, THIRD_ADDRESS);
	}

	@Test
	void staysWithinTheBudgetWhileManyAddressesJoinOnePot() {
		LoginPots tested = potsHolding(TWO_POTS);

		for (int address = 0; address < MANY_ADDRESSES; address++) {
			tested.turnsAway("10.0.1." + address, AURORA);
		}

		List<String> tracked = tested.trackedAddresses();

		assertThat(tracked).hasSizeLessThanOrEqualTo(TWO_POTS);
	}

	@Test
	void staysWithinTheBudgetWhileManyAccountsJoinOnePot() {
		LoginPots tested = potsHolding(TWO_POTS);

		for (int account = 0; account < MANY_ADDRESSES; account++) {
			tested.turnsAway(FIRST_ADDRESS, "Account" + account);
		}

		List<String> tracked = tested.trackedAccounts();

		assertThat(tracked).hasSizeLessThanOrEqualTo(TWO_POTS);
	}

	@Test
	void staysWithinTheBudgetWhileManyPotsAppear() {
		LoginPots tested = potsHolding(TWO_POTS);

		for (int pot = 0; pot < MANY_ADDRESSES; pot++) {
			tested.turnsAway("10.0.1." + pot, "Account" + pot);
		}

		List<String> tracked = tested.trackedAddresses();

		assertThat(tracked).hasSize(TWO_POTS);
	}

	@Test
	void logsTheAddressWhoseRequestTurnsAPotAwayOnce() {
		LoggerSpy logger = new LoggerSpy();
		LoginPots tested = potsHolding(TWO_POTS, logger);
		anyTurnedAway(tested, THREE_REQUESTS_PER_COUNT, FIRST_ADDRESS, AURORA);

		tested.turnsAway(SECOND_ADDRESS, AURORA);
		tested.turnsAway(SECOND_ADDRESS, AURORA);

		assertThat(logger.infoMessages()).containsExactly("Blocked:" + SECOND_ADDRESS);
	}

	@Test
	void countsTheCountFromTheFirstRequestOfThePotNotFromTheLast() {
		LoginPots tested = potsHolding(TWO_POTS);
		tested.turnsAway(FIRST_ADDRESS, AURORA);
		clock.advanceBy(Duration.ofSeconds(6));
		anyTurnedAway(tested, THREE_REQUESTS_PER_COUNT - 1, FIRST_ADDRESS, AURORA);
		clock.advanceBy(Duration.ofSeconds(4));

		boolean turnedAway = tested.turnsAway(FIRST_ADDRESS, AURORA);

		assertThat(turnedAway).isFalse();
	}

	@Test
	void concurrentRequestsLoseNoCount() throws InterruptedException {
		int threadCount = 20;
		int requestsPerThread = 50;
		LoginPots tested = potsHolding(threadCount * requestsPerThread, TWO_POTS, new LoggerSpy());
		CountDownLatch startGate = new CountDownLatch(1);
		CountDownLatch done = new CountDownLatch(threadCount);
		ExecutorService pool = Executors.newFixedThreadPool(threadCount);
		for (int thread = 0; thread < threadCount; thread++) {
			pool.submit(() -> {
				try {
					startGate.await();
					for (int request = 0; request < requestsPerThread; request++) {
						tested.turnsAway(FIRST_ADDRESS, AURORA);
					}
				} catch (InterruptedException interrupted) {
					Thread.currentThread().interrupt();
				} finally {
					done.countDown();
				}
			});
		}
		startGate.countDown();
		done.await();
		pool.shutdown();

		boolean turnedAway = tested.turnsAway(FIRST_ADDRESS, AURORA);

		assertThat(turnedAway).isTrue();
	}

	private boolean anyTurnedAway(LoginPots pots, long requests, String address, String account) {
		boolean anyTurnedAway = false;
		for (long request = 0; request < requests; request++) {
			anyTurnedAway |= pots.turnsAway(address, account);
		}
		return anyTurnedAway;
	}

	private LoginPots potsHolding(int maxTrackedClients) {
		return potsHolding(maxTrackedClients, new LoggerSpy());
	}

	private LoginPots potsHolding(int maxTrackedClients, LoggerSpy logger) {
		return potsHolding(THREE_REQUESTS_PER_COUNT, maxTrackedClients, logger);
	}

	private LoginPots potsHolding(long requestsPerCount, int maxTrackedClients, LoggerSpy logger) {
		RateLimitConfiguration configuration = new RateLimitConfiguration(requestsPerCount, COUNT, BLOCK_DURATION, maxTrackedClients, TEN_AT_ONCE, COUNT);

		return new LoginPots(configuration, clock, logger);
	}
}
