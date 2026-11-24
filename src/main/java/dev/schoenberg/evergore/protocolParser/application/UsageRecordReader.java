package dev.schoenberg.evergore.protocolParser.application;

import java.time.Instant;
import java.util.Map;

import dev.schoenberg.evergore.protocolParser.businessLogic.usage.UsageKind;

public interface UsageRecordReader {
	Map<String, Map<UsageKind, Integer>> countsSince(Instant from);
}
