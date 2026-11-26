package dev.schoenberg.evergore.protocolParser.application;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import dev.schoenberg.evergore.protocolParser.businessLogic.usage.UsageRecord;

import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.silentThrow;

class RecordingUsageRecords implements UsageRecordRepository {
	static final IllegalStateException FAILURE = new IllegalStateException("the disk is full");
	static final Error BREAK = new Error("the writer thread breaks");

	private final List<List<UsageRecord>> batches = new CopyOnWriteArrayList<>();
	private final AtomicInteger batchesTaken = new AtomicInteger();
	private final AtomicBoolean failsTheFirstBatch = new AtomicBoolean();
	private final AtomicBoolean holdsTheFirstBatch = new AtomicBoolean();
	private final AtomicBoolean holdsTheSecondBatch = new AtomicBoolean();
	private final AtomicBoolean breaksTheFirstBatch = new AtomicBoolean();
	private final AtomicBoolean breaksTheSecondBatch = new AtomicBoolean();
	private final CountDownLatch firstBatchWriting = new CountDownLatch(1);
	private final CountDownLatch firstBatchReleased = new CountDownLatch(1);
	private final CountDownLatch secondBatchWriting = new CountDownLatch(1);
	private final CountDownLatch secondBatchReleased = new CountDownLatch(1);

	@Override
	public void addAll(List<UsageRecord> batch) {
		int number = batchesTaken.incrementAndGet();
		boolean second = number == 2;
		if (failsTheFirstBatch.getAndSet(false)) {
			firstBatchWriting.countDown();
			throw FAILURE;
		}
		if (holdsTheFirstBatch.getAndSet(false)) {
			firstBatchWriting.countDown();
			silentThrow(() -> firstBatchReleased.await());
		}
		if (number == 1 && breaksTheFirstBatch.get()) {
			throw BREAK;
		}
		if (second && breaksTheSecondBatch.get()) {
			throw BREAK;
		}
		if (second && holdsTheSecondBatch.get()) {
			secondBatchWriting.countDown();
			silentThrow(() -> secondBatchReleased.await());
		}
		batches.add(List.copyOf(batch));
	}

	void failTheFirstBatch() {
		failsTheFirstBatch.set(true);
	}

	void holdTheFirstBatch() {
		holdsTheFirstBatch.set(true);
	}

	void breakTheFirstBatch() {
		breaksTheFirstBatch.set(true);
	}

	void holdAndBreakTheFirstBatch() {
		holdsTheFirstBatch.set(true);
		breaksTheFirstBatch.set(true);
	}

	void awaitTheFirstBatchWriting() {
		silentThrow(() -> firstBatchWriting.await());
	}

	void releaseTheFirstBatch() {
		firstBatchReleased.countDown();
	}

	void breakTheSecondBatch() {
		breaksTheSecondBatch.set(true);
	}

	void holdTheSecondBatch() {
		holdsTheSecondBatch.set(true);
	}

	void awaitTheSecondBatchWriting() {
		silentThrow(() -> secondBatchWriting.await());
	}

	void releaseTheSecondBatch() {
		secondBatchReleased.countDown();
	}

	List<List<UsageRecord>> batches() {
		return List.copyOf(batches);
	}

	List<UsageRecord> records() {
		return batches.stream().flatMap(List::stream).toList();
	}
}
