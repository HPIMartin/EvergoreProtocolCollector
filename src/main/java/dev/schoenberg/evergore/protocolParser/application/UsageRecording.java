package dev.schoenberg.evergore.protocolParser.application;

import dev.schoenberg.evergore.protocolParser.businessLogic.usage.UsageRecord;

public interface UsageRecording {
	void record(UsageRecord record);
}
