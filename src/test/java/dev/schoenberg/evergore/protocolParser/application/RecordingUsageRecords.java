package dev.schoenberg.evergore.protocolParser.application;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import dev.schoenberg.evergore.protocolParser.businessLogic.usage.UsageRecord;

import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.silentThrow;

class RecordingUsageRecords implements UsageRecordRepository {
	private final List<List<UsageRecord>> batches = new CopyOnWriteArrayList<>();
	private final AtomicBoolean holdsTheFirstBatch = new AtomicBoolean();
	private final CountDownLatch firstBatchWriting = new CountDownLatch(1);
	private final CountDownLatch firstBatchReleased = new CountDownLatch(1);

	@Override
	public void addAll(List<UsageRecord> batch) {
		if (holdsTheFirstBatch.getAndSet(false)) {
			firstBatchWriting.countDown();
			silentThrow(() -> firstBatchReleased.await());
		}
		batches.add(List.copyOf(batch));
	}

	void holdTheFirstBatch() {
		holdsTheFirstBatch.set(true);
	}

	void awaitTheFirstBatchWriting() {
		silentThrow(() -> firstBatchWriting.await());
	}

	void releaseTheFirstBatch() {
		firstBatchReleased.countDown();
	}

	List<List<UsageRecord>> batches() {
		return List.copyOf(batches);
	}

	List<UsageRecord> records() {
		return batches.stream().flatMap(List::stream).toList();
	}
}
