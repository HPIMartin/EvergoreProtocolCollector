package dev.schoenberg.evergore.protocolParser.application;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.Set;

import dev.schoenberg.evergore.protocolParser.businessLogic.usage.UsageKind;

public interface UsageRecordReader {
	Map<String, Map<UsageKind, Integer>> countsSince(Instant from);

	Map<String, Set<LocalDate>> daysUsedSince(Instant from, ZoneId zone);
}
