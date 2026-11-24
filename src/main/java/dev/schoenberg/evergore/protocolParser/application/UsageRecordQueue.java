package dev.schoenberg.evergore.protocolParser.application;

import java.time.Duration;
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
	private final Duration closeBound;
	private final ThreadEnd threadEnd;
	private final Logger logger;
	private Thread writer;
	private volatile boolean closed;
	private volatile int inWrite;

	private UsageRecordQueue(UsageRecordRepository repository, UsageWriting writing, ThreadEnd threadEnd, Logger logger) {
		this.repository = repository;
		this.batchSize = writing.batchSize();
		this.closeBound = writing.closeBound();
		this.freePlaces = new Semaphore(writing.capacity());
		this.threadEnd = threadEnd;
		this.logger = logger;
	}

	public static UsageRecordQueue start(UsageRecordRepository repository, UsageWriting writing, ThreadFactory threads, ThreadEnd threadEnd, Logger logger) {
		UsageRecordQueue queue = new UsageRecordQueue(repository, writing, threadEnd, logger);
		queue.writer = threads.newThread(queue::writeUntilTheEnd);
		queue.writer.start();
		return queue;
	}

	public void record(UsageRecord record) {
		if (!closed && freePlaces.tryAcquire()) {
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
		closed = true;
		waiting.add(END);
		if (!silentThrow(() -> threadEnd.awaited(writer, closeBound))) {
			long notWritten = inWrite + waiting.stream().filter(record -> record != END).count();
			lost.addAndGet(notWritten);
			logger.warn("Closed with " + notWritten + " usage records not written");
		}
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
				write(batch);
			}
		}
	}

	private void write(List<UsageRecord> batch) {
		inWrite = batch.size();
		try {
			repository.addAll(batch);
		} catch (RuntimeException failure) {
			lost.addAndGet(batch.size());
			logger.error("Lost " + batch.size() + " usage records: their write failed", failure);
		} finally {
			inWrite = 0;
		}
	}
}
