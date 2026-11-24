package dev.schoenberg.evergore.protocolParser.application;

import java.util.List;

import dev.schoenberg.evergore.protocolParser.businessLogic.usage.UsageRecord;

public interface UsageRecordRepository {
	void addAll(List<UsageRecord> batch);
}
