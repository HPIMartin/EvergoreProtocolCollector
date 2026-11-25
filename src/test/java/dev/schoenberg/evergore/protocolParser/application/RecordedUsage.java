package dev.schoenberg.evergore.protocolParser.application;

import java.util.ArrayList;
import java.util.List;

import dev.schoenberg.evergore.protocolParser.businessLogic.usage.UsageRecord;

public class RecordedUsage implements UsageRecording {
	private final List<UsageRecord> records = new ArrayList<>();

	@Override
	public void record(UsageRecord record) {
		records.add(record);
	}

	public List<UsageRecord> records() {
		return List.copyOf(records);
	}
}
