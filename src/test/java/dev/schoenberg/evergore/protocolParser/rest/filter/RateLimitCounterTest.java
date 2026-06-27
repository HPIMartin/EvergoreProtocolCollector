package dev.schoenberg.evergore.protocolParser.rest.filter;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitCounterTest {

	private static final Duration INTERVAL = Duration.ofSeconds(10);
	private static final Duration BLOCK_DURATION = Duration.ofMinutes(1);

	@Test
	void blockExpiresAfterConfiguredDuration() {
		AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-01-01T00:00:00Z"));
		Clock clock = clockFrom(now);
		RateLimitCounter counter = new RateLimitCounter(INTERVAL, BLOCK_DURATION, clock);

		counter.block();

		assertThat(counter.isBlocked()).isTrue();

		now.set(now.get().plus(BLOCK_DURATION).plusSeconds(1));

		assertThat(counter.isBlocked()).isFalse();
	}

	@Test
	void blockRemainsActiveBeforeBlockDurationExpires() {
		AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-01-01T00:00:00Z"));
		Clock clock = clockFrom(now);
		RateLimitCounter counter = new RateLimitCounter(INTERVAL, BLOCK_DURATION, clock);

		counter.block();

		now.set(now.get().plus(BLOCK_DURATION).minusSeconds(1));

		assertThat(counter.isBlocked()).isTrue();
	}

	@Test
	void concurrentBlockCallsDoNotCorruptBlockState() throws InterruptedException {
		AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-01-01T00:00:00Z"));
		Clock clock = clockFrom(now);
		RateLimitCounter counter = new RateLimitCounter(INTERVAL, BLOCK_DURATION, clock);

		int threadCount = 20;
		CountDownLatch startGate = new CountDownLatch(1);
		CountDownLatch done = new CountDownLatch(threadCount);
		List<Throwable> errors = new ArrayList<>();

		ExecutorService pool = Executors.newFixedThreadPool(threadCount);
		for (int i = 0; i < threadCount; i++) {
			pool.submit(() -> {
				try {
					startGate.await();
					counter.block();
				} catch (Throwable t) {
					synchronized (errors) {
						errors.add(t);
					}
				} finally {
					done.countDown();
				}
			});
		}

		startGate.countDown();
		done.await();
		pool.shutdown();

		assertThat(errors).isEmpty();
		assertThat(counter.isBlocked()).isTrue();
	}

	private static Clock clockFrom(AtomicReference<Instant> now) {
		return new Clock() {
			@Override
			public ZoneOffset getZone() {
				return ZoneOffset.UTC;
			}

			@Override
			public Clock withZone(java.time.ZoneId zone) {
				return this;
			}

			@Override
			public Instant instant() {
				return now.get();
			}
		};
	}
}
