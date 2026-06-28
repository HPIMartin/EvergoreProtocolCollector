package dev.schoenberg.evergore.protocolParser.application;

import java.time.*;
import java.util.*;

public class LastRunStatus {

	private volatile Instant lastSuccessfulRunInstant;

	public void recordSuccessfulRun(Instant when) {
		lastSuccessfulRunInstant = when;
	}

	public Optional<Instant> lastSuccessfulRun() {
		return Optional.ofNullable(lastSuccessfulRunInstant);
	}
}
