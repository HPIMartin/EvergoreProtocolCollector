package dev.schoenberg.evergore.protocolParser.application;

import java.time.*;
import java.util.*;

public class LastRunStatus {

	private volatile Instant lastSuccessfulRunInstant;
	private volatile List<String> unknownItemNames = List.of();

	public void recordSuccessfulRun(Instant when) {
		lastSuccessfulRunInstant = when;
	}

	public Optional<Instant> lastSuccessfulRun() {
		return Optional.ofNullable(lastSuccessfulRunInstant);
	}

	public void recordUnknownItems(List<String> names) {
		unknownItemNames = List.copyOf(names);
	}

	public List<String> unknownItemNames() {
		return unknownItemNames;
	}
}
