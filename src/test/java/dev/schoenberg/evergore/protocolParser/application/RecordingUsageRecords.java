package dev.schoenberg.evergore.protocolParser.application;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import dev.schoenberg.evergore.protocolParser.businessLogic.usage.UsageRecord;

class RecordingUsageRecords implements UsageRecordRepository {
	private final List<List<UsageRecord>> batches = new CopyOnWriteArrayList<>();

	@Override
	public void addAll(List<UsageRecord> batch) {
		batches.add(List.copyOf(batch));
	}

	List<List<UsageRecord>> batches() {
		return List.copyOf(batches);
	}

	List<UsageRecord> records() {
		return batches.stream().flatMap(List::stream).toList();
	}
}
