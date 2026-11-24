package dev.schoenberg.evergore.protocolParser.application;

import java.time.Instant;

public sealed interface StartOutcome {
	record Started() implements StartOutcome {}

	record AlreadyRunning(Instant since) implements StartOutcome {}
}
