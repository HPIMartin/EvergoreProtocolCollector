package dev.schoenberg.evergore.protocolParser.rest.filter;

import java.time.Duration;
import java.time.Instant;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitCounterTest {

	private static final Duration INTERVAL = Duration.ofSeconds(10);
	private static final Duration BLOCK_DURATION = Duration.ofMinutes(1);
	private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");

	private final MutableClock clock = new MutableClock(START);
	private final RateLimitCounter tested = new RateLimitCounter(INTERVAL, BLOCK_DURATION, clock);

	@Test
	void blockExpiresAfterConfiguredDuration() {
		tested.block();
		clock.advanceBy(BLOCK_DURATION.plusSeconds(1));

		boolean blocked = tested.isBlocked();

		assertThat(blocked).isFalse();
	}

	@Test
	void blockRemainsActiveBeforeBlockDurationExpires() {
		tested.block();
		clock.advanceBy(BLOCK_DURATION.minusSeconds(1));

		boolean blocked = tested.isBlocked();

		assertThat(blocked).isTrue();
	}

	@Test
	void isIdleOnceTheIntervalElapsedWithoutARequest() {
		clock.advanceBy(INTERVAL);

		boolean idle = tested.isIdle();

		assertThat(idle).isTrue();
	}

	@Test
	void isNotIdleWhileTheIntervalIsStillRunning() {
		tested.increment();
		clock.advanceBy(INTERVAL.minusSeconds(1));

		boolean idle = tested.isIdle();

		assertThat(idle).isFalse();
	}

	@Test
	void isNotIdleWhileBlockedEvenAfterTheIntervalElapsed() {
		tested.block();
		clock.advanceBy(INTERVAL);

		boolean idle = tested.isIdle();

		assertThat(idle).isFalse();
	}

	@Test
	void isIdleAgainOnceTheBlockExpired() {
		tested.block();
		clock.advanceBy(BLOCK_DURATION.plusSeconds(1));

		boolean idle = tested.isIdle();

		assertThat(idle).isTrue();
	}

	@Test
	void concurrentIncrementsLoseNoRequest() throws InterruptedException {
		int threadCount = 20;
		int requestsPerThread = 50;
		CountDownLatch startGate = new CountDownLatch(1);
		CountDownLatch done = new CountDownLatch(threadCount);
		Queue<Long> counted = new ConcurrentLinkedQueue<>();

		ExecutorService pool = Executors.newFixedThreadPool(threadCount);
		for (int thread = 0; thread < threadCount; thread++) {
			pool.submit(() -> {
				try {
					startGate.await();
					for (int request = 0; request < requestsPerThread; request++) {
						counted.add(tested.increment());
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

		assertThat(counted).doesNotHaveDuplicates().hasSize(threadCount * requestsPerThread);
	}
}
