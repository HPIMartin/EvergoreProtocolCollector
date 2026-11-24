package dev.schoenberg.evergore.protocolParser.acceptance.world;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CollectionRuns {
	private static final String DAILY = "the daily collection";
	private static final String STARTED_AT = "the collection started at ";
	private static final String THAT = "that collection";
	private static final int CLOCK_TIME_LENGTH = 5;

	private final List<LocalDateTime> startedByHand = new ArrayList<>();
	private final Map<LocalDateTime, Integer> logSizeAtStart = new HashMap<>();
	private LocalDateTime dailyStart;
	private LocalDateTime lastStart;

	public void dailyStarted(LocalDateTime at, int logSize) {
		dailyStart = at;
		started(at, logSize);
	}

	public void startedByHand(LocalDateTime at, int logSize) {
		startedByHand.add(at);
		started(at, logSize);
	}

	public LocalDateTime dailyStart() {
		return known(dailyStart, DAILY);
	}

	public LocalDateTime lastStart() {
		return known(lastStart, THAT);
	}

	public LocalDateTime startNamed(String phrase) {
		if (phrase.startsWith(DAILY)) {
			return dailyStart();
		}
		if (phrase.startsWith(STARTED_AT)) {
			return startedByHandAt(LocalTime.parse(phrase.substring(STARTED_AT.length(), STARTED_AT.length() + CLOCK_TIME_LENGTH)));
		}
		if (phrase.equals(THAT)) {
			return lastStart();
		}
		throw new IllegalArgumentException("The scenario names no collection by \"" + phrase + "\"");
	}

	public int logSizeAtStartOf(LocalDateTime at) {
		return known(logSizeAtStart.get(at), "the collection of " + at);
	}

	private LocalDateTime startedByHandAt(LocalTime clockTime) {
		return startedByHand
				.reversed()
				.stream()
				.filter(start -> start.toLocalTime().equals(clockTime))
				.findFirst()
				.orElseThrow(() -> new AssertionError("No collection was started by hand at " + clockTime));
	}

	private void started(LocalDateTime at, int logSize) {
		lastStart = at;
		logSizeAtStart.put(at, logSize);
	}

	private static <T> T known(T value, String what) {
		if (value == null) {
			throw new AssertionError("The scenario started no collection of the kind " + what);
		}
		return value;
	}
}
