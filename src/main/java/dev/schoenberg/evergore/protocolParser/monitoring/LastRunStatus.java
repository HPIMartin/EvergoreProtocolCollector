package dev.schoenberg.evergore.protocolParser.monitoring;

import java.time.*;
import java.util.*;

import jakarta.inject.*;

@Singleton
public class LastRunStatus {

	private volatile Instant lastSuccessfulRunInstant;

	public void recordSuccessfulRun(Instant when) {
		lastSuccessfulRunInstant = when;
	}

	public Optional<Instant> lastSuccessfulRun() {
		return Optional.ofNullable(lastSuccessfulRunInstant);
	}
}
