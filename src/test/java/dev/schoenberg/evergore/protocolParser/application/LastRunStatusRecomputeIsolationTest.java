package dev.schoenberg.evergore.protocolParser.application;

import java.time.Instant;
import java.util.AbstractList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import dev.schoenberg.evergore.protocolParser.application.LastRunStatus.Snapshot;

import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.silentThrow;
import static org.assertj.core.api.Assertions.assertThat;

class LastRunStatusRecomputeIsolationTest {
	private static final long HANG_GUARD_SECONDS = 30;
	private static final Instant RUN_ONE_INSTANT = Instant.parse("2026-06-21T08:00:00Z");
	private static final Instant RUN_TWO_INSTANT = Instant.parse("2026-06-21T09:00:00Z");
	private static final int WRITES_PER_WRITER_THREAD = 3_000;

	private final CountDownLatch writerIsBuildingItsNewSnapshot = new CountDownLatch(1);
	private final CountDownLatch readerHasTakenItsSnapshot = new CountDownLatch(1);

	@Test
	void answersTheSnapshotOfTheRunBeforeARecomputeWhileThatRecomputeIsStillCopyingItsLastListBeforePublishing() throws InterruptedException {
		LastRunStatus tested = new LastRunStatus();
		tested.recordSuccessfulRecompute(RUN_ONE_INSTANT, List.of("old-item"), List.of(), List.of("old-avatar"));
		List<String> blockingFailedAvatarNames = new BlockingOnFirstIteration(List.of("new-avatar"));

		Thread writer = new Thread(() -> tested.recordSuccessfulRecompute(RUN_TWO_INSTANT, List.of("new-item"), List.of(), blockingFailedAvatarNames));
		writer.start();
		boolean writerReachedTheBlockingCopy = writerIsBuildingItsNewSnapshot.await(HANG_GUARD_SECONDS, TimeUnit.SECONDS);

		Snapshot readDuringTheWrite = tested.snapshot();
		readerHasTakenItsSnapshot.countDown();
		writer.join();
		Snapshot readAfterTheWrite = tested.snapshot();

		assertThat(writerReachedTheBlockingCopy).as("the writer never reached its blocking copy").isTrue();
		assertThat(readDuringTheWrite.lastSuccessfulRecompute()).as("a read landing inside an open recompute must see the state before it, whole").contains(RUN_ONE_INSTANT);
		assertThat(readDuringTheWrite.unknownItemNames())
				.as("a read landing inside an open recompute must not see the new list already published under a torn writer")
				.containsExactly("old-item");
		assertThat(readDuringTheWrite.failedAvatarNames()).containsExactly("old-avatar");
		assertThat(readAfterTheWrite.lastSuccessfulRecompute()).as("once the recompute committed, the next read sees it whole").contains(RUN_TWO_INSTANT);
		assertThat(readAfterTheWrite.unknownItemNames()).containsExactly("new-item");
		assertThat(readAfterTheWrite.failedAvatarNames()).containsExactly("new-avatar");
	}

	@Test
	void neverMixesFieldsOfTwoRacingRecomputesRegardlessOfWriteOrder() throws InterruptedException {
		LastRunStatus tested = new LastRunStatus();
		AtomicBoolean writersDone = new AtomicBoolean(false);
		AtomicReference<Throwable> readerFailure = new AtomicReference<>();
		AtomicInteger taggedSnapshotsObserved = new AtomicInteger();
		CyclicBarrier start = new CyclicBarrier(3);

		Thread writerA = new Thread(() -> raceWrites(tested, "A", start));
		Thread writerB = new Thread(() -> raceWrites(tested, "B", start));
		Thread reader = new Thread(() -> {
			silentThrow(() -> start.await());
			while (!writersDone.get()) {
				assertRunTaggedFieldsBelongToTheSameRun(tested.snapshot(), readerFailure, taggedSnapshotsObserved);
			}
			assertRunTaggedFieldsBelongToTheSameRun(tested.snapshot(), readerFailure, taggedSnapshotsObserved);
		});
		reader.setUncaughtExceptionHandler((thread, error) -> readerFailure.compareAndSet(null, error));

		writerA.start();
		writerB.start();
		reader.start();
		writerA.join();
		writerB.join();
		int taggedSnapshotsObservedWhileRacing = taggedSnapshotsObserved.get();
		writersDone.set(true);
		reader.join();

		assertThat(readerFailure.get()).as("the reader observed a mixed snapshot, or died instead of asserting").isNull();
		assertThat(taggedSnapshotsObservedWhileRacing).as("the reader never observed a tagged snapshot while the writers were still racing, so it proved nothing").isPositive();
	}

	private static void raceWrites(LastRunStatus tested, String tag, CyclicBarrier start) {
		silentThrow(() -> start.await());
		Instant runInstant = tag.equals("A") ? RUN_ONE_INSTANT : RUN_TWO_INSTANT;
		for (int i = 0; i < WRITES_PER_WRITER_THREAD; i++) {
			tested.recordSuccessfulRecompute(runInstant, List.of(tag + "-item"), List.of(), List.of(tag + "-avatar"));
		}
	}

	private static void assertRunTaggedFieldsBelongToTheSameRun(Snapshot snapshot, AtomicReference<Throwable> readerFailure, AtomicInteger taggedSnapshotsObserved) {
		if (readerFailure.get() != null) {
			return;
		}
		List<String> unknown = snapshot.unknownItemNames();
		List<String> failed = snapshot.failedAvatarNames();
		if (unknown.isEmpty() && failed.isEmpty()) {
			return;
		}
		taggedSnapshotsObserved.incrementAndGet();
		String unknownTag = unknown.isEmpty() ? null : unknown.get(0).substring(0, 1);
		String failedTag = failed.isEmpty() ? null : failed.get(0).substring(0, 1);
		try {
			assertThat(unknownTag).as("unknownItemNames tag must match failedAvatarNames tag within one snapshot").isEqualTo(failedTag);
			String instantTag = snapshot.lastSuccessfulRecompute().map(instant -> instant.equals(RUN_ONE_INSTANT) ? "A" : "B").orElse(null);
			assertThat(instantTag).as("instant tag must match the field tags within one snapshot").isEqualTo(unknownTag != null ? unknownTag : failedTag);
		} catch (AssertionError e) {
			readerFailure.compareAndSet(null, e);
		}
	}

	private class BlockingOnFirstIteration extends AbstractList<String> {
		private final List<String> delegate;

		private BlockingOnFirstIteration(List<String> delegate) {
			this.delegate = delegate;
		}

		@Override
		public Iterator<String> iterator() {
			writerIsBuildingItsNewSnapshot.countDown();
			silentThrow(() -> readerHasTakenItsSnapshot.await(HANG_GUARD_SECONDS, TimeUnit.SECONDS));
			return delegate.iterator();
		}

		@Override
		public String get(int index) {
			return delegate.get(index);
		}

		@Override
		public int size() {
			return delegate.size();
		}
	}
}
