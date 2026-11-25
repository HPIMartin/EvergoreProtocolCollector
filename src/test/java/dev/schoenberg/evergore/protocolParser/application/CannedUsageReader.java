package dev.schoenberg.evergore.protocolParser.application;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import dev.schoenberg.evergore.protocolParser.businessLogic.Constants;
import dev.schoenberg.evergore.protocolParser.businessLogic.usage.UsageKind;

class CannedUsageReader implements UsageRecordReader {
	private final Map<Instant, Map<String, Map<UsageKind, Integer>>> counts = new HashMap<>();
	private final Map<Instant, Map<String, Set<LocalDate>>> days = new HashMap<>();
	private final Map<String, Instant> latest = new HashMap<>();
	private Optional<Instant> first = Optional.empty();

	void counts(Instant from, String account, UsageKind kind, int count) {
		counts.computeIfAbsent(from, _ -> new HashMap<>()).computeIfAbsent(account, _ -> new EnumMap<>(UsageKind.class)).put(kind, count);
	}

	void days(Instant from, String account, LocalDate... used) {
		days.computeIfAbsent(from, _ -> new HashMap<>()).computeIfAbsent(account, _ -> new HashSet<>()).addAll(List.of(used));
	}

	void latest(String account, Instant at) {
		latest.put(account, at);
	}

	void first(Instant at) {
		first = Optional.of(at);
	}

	@Override
	public Map<String, Map<UsageKind, Integer>> countsSince(Instant from) {
		return counts.getOrDefault(from, Map.of());
	}

	@Override
	public Map<String, Set<LocalDate>> daysUsedSince(Instant from, ZoneId zone) {
		if (!zone.equals(Constants.APP_ZONE)) {
			throw new AssertionError("The days of use are asked in " + zone + ", not in " + Constants.APP_ZONE);
		}
		return days.getOrDefault(from, Map.of());
	}

	@Override
	public Map<String, Instant> latestUses() {
		return Map.copyOf(latest);
	}

	@Override
	public Optional<Instant> firstRecord() {
		return first;
	}
}
