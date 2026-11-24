package dev.schoenberg.evergore.protocolParser.application;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

import dev.schoenberg.evergore.protocolParser.Logger;
import dev.schoenberg.evergore.protocolParser.businessLogic.usage.UsageRecord;

import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.silentThrow;

public class UsageRecordQueue implements AutoCloseable {
	private static final UsageRecord END = new UsageRecord("", Instant.EPOCH, null, Optional.empty());

	private final BlockingQueue<UsageRecord> waiting = new LinkedBlockingQueue<>();
	private final AtomicLong lost = new AtomicLong();
	private final Semaphore freePlaces;
	private final UsageRecordRepository repository;
	private final int batchSize;
	private Thread writer;

	private UsageRecordQueue(UsageRecordRepository repository, UsageWriting writing) {
		this.repository = repository;
		this.batchSize = writing.batchSize();
		this.freePlaces = new Semaphore(writing.capacity());
	}

	public static UsageRecordQueue start(UsageRecordRepository repository, UsageWriting writing, ThreadFactory threads, Logger logger) {
		UsageRecordQueue queue = new UsageRecordQueue(repository, writing);
		queue.writer = threads.newThread(queue::writeUntilTheEnd);
		queue.writer.start();
		return queue;
	}

	public void record(UsageRecord record) {
		if (freePlaces.tryAcquire()) {
			waiting.add(record);
		} else {
			lost.incrementAndGet();
		}
	}

	public long lostSinceStart() {
		return lost.get();
	}

	@Override
	public void close() {
		waiting.add(END);
		silentThrow(() -> writer.join());
	}

	private void writeUntilTheEnd() {
		boolean ended = false;
		while (!ended) {
			List<UsageRecord> batch = new ArrayList<>();
			batch.add(silentThrow(waiting::take));
			waiting.drainTo(batch, batchSize - 1);
			ended = batch.removeIf(record -> record == END);
			freePlaces.release(batch.size());
			if (!batch.isEmpty()) {
				repository.addAll(batch);
			}
		}
	}
}
