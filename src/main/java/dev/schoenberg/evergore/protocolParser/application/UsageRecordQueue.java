package dev.schoenberg.evergore.protocolParser.application;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;

import dev.schoenberg.evergore.protocolParser.Logger;
import dev.schoenberg.evergore.protocolParser.businessLogic.usage.UsageRecord;

import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.silentThrow;

public class UsageRecordQueue implements AutoCloseable {
	private static final UsageRecord END = new UsageRecord("", Instant.EPOCH, null, Optional.empty());

	private final BlockingQueue<UsageRecord> waiting = new LinkedBlockingQueue<>();
	private final UsageRecordRepository repository;
	private final int batchSize;
	private Thread writer;

	private UsageRecordQueue(UsageRecordRepository repository, int batchSize) {
		this.repository = repository;
		this.batchSize = batchSize;
	}

	public static UsageRecordQueue start(UsageRecordRepository repository, UsageWriting writing, ThreadFactory threads, Logger logger) {
		UsageRecordQueue queue = new UsageRecordQueue(repository, writing.batchSize());
		queue.writer = threads.newThread(queue::writeUntilTheEnd);
		queue.writer.start();
		return queue;
	}

	public void record(UsageRecord record) {
		waiting.add(record);
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
			if (!batch.isEmpty()) {
				repository.addAll(batch);
			}
		}
	}
}
