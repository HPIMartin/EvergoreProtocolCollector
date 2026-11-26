package dev.schoenberg.evergore.protocolParser.application;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

import dev.schoenberg.evergore.protocolParser.Logger;
import dev.schoenberg.evergore.protocolParser.businessLogic.usage.UsageRecord;

import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.silentThrow;

public class UsageRecordQueue implements UsageRecording, LostUsageRecords, AutoCloseable {
	private static final Waiting END = new Waiting() {
		@Override
		public boolean ends() {
			return true;
		}
	};

	private final BlockingQueue<Waiting> waiting = new LinkedBlockingQueue<>();
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

	@Override
	public void record(UsageRecord record) {
		if (!closed && freePlaces.tryAcquire()) {
			waiting.add(new Recorded(record));
		} else {
			lost.incrementAndGet();
		}
	}

	public void awaitWritten() {
		if (closed) {
			return;
		}
		Mark mark = new Mark(new CountDownLatch(1));
		waiting.add(mark);
		silentThrow(() -> mark.written().await());
	}

	@Override
	public long lostSinceStart() {
		return lost.get();
	}

	@Override
	public void close() {
		closed = true;
		waiting.add(END);
		boolean ended = silentThrow(() -> threadEnd.awaited(writer, closeBound));
		long notWritten = (ended ? 0 : inWrite) + waiting.stream().mapToInt(Waiting::records).sum();
		if (notWritten > 0) {
			lost.addAndGet(notWritten);
			logger.warn("Closed with " + notWritten + " usage records not written");
		}
	}

	private void writeUntilTheEnd() {
		boolean ended = false;
		while (!ended) {
			List<Waiting> taken = new ArrayList<>();
			taken.add(silentThrow(waiting::take));
			waiting.drainTo(taken, batchSize - 1);
			List<UsageRecord> batch = new ArrayList<>();
			taken.forEach(entry -> entry.addTo(batch));
			ended = taken.stream().anyMatch(Waiting::ends);
			freePlaces.release(batch.size());
			if (!batch.isEmpty()) {
				write(batch);
			}
			taken.forEach(Waiting::passed);
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

	private interface Waiting {
		default int records() {
			return 0;
		}

		default void addTo(List<UsageRecord> batch) {}

		default boolean ends() {
			return false;
		}

		default void passed() {}
	}

	private record Mark(CountDownLatch written) implements Waiting {
		@Override
		public void passed() {
			written.countDown();
		}
	}

	private record Recorded(UsageRecord record) implements Waiting {
		@Override
		public int records() {
			return 1;
		}

		@Override
		public void addTo(List<UsageRecord> batch) {
			batch.add(record);
		}
	}
}
