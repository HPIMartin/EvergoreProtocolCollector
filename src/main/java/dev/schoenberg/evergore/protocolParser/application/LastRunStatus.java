package dev.schoenberg.evergore.protocolParser.application;

import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;

public class LastRunStatus {

	private final AtomicReference<Snapshot> current = new AtomicReference<>(Snapshot.empty());

	public void recordSuccessfulScrape(Instant when) {
		current.updateAndGet(snapshot -> snapshot.withLastSuccessfulScrape(when));
	}

	public void recordScrapeFailure(Instant when) {
		current.updateAndGet(snapshot -> snapshot.withLastScrapeFailure(when));
	}

	public void recordSuccessfulRecompute(Instant when, List<String> unknownItemNames, List<String> failedAvatarNames) {
		List<String> unknown = List.copyOf(unknownItemNames);
		List<String> failed = List.copyOf(failedAvatarNames);
		current.updateAndGet(snapshot -> snapshot.withSuccessfulRecompute(when, unknown, failed));
	}

	public void recordRecomputeFailure(Instant when) {
		current.updateAndGet(snapshot -> snapshot.withLastRecomputeFailure(when));
	}

	public Snapshot snapshot() {
		return current.get();
	}

	public record Snapshot(Instant lastSuccessfulScrapeInstant, Instant lastScrapeFailureInstant, Instant lastSuccessfulRecomputeInstant, Instant lastRecomputeFailureInstant,
			boolean recomputeHealthy, List<String> unknownItemNames, List<String> failedAvatarNames) {

		public Snapshot {
			unknownItemNames = List.copyOf(unknownItemNames);
			failedAvatarNames = List.copyOf(failedAvatarNames);
		}

		public Optional<Instant> lastSuccessfulScrape() {
			return Optional.ofNullable(lastSuccessfulScrapeInstant);
		}

		public Optional<Instant> lastScrapeFailure() {
			return Optional.ofNullable(lastScrapeFailureInstant);
		}

		public Optional<Instant> lastSuccessfulRecompute() {
			return Optional.ofNullable(lastSuccessfulRecomputeInstant);
		}

		public Optional<Instant> lastRecomputeFailure() {
			return Optional.ofNullable(lastRecomputeFailureInstant);
		}

		private static Snapshot empty() {
			return new Snapshot(null, null, null, null, false, List.of(), List.of());
		}

		private Snapshot withLastSuccessfulScrape(Instant when) {
			return new Snapshot(when, lastScrapeFailureInstant, lastSuccessfulRecomputeInstant, lastRecomputeFailureInstant, recomputeHealthy, unknownItemNames, failedAvatarNames);
		}

		private Snapshot withLastScrapeFailure(Instant when) {
			return new Snapshot(lastSuccessfulScrapeInstant, when, lastSuccessfulRecomputeInstant, lastRecomputeFailureInstant, recomputeHealthy, unknownItemNames,
					failedAvatarNames);
		}

		private Snapshot withLastRecomputeFailure(Instant when) {
			return new Snapshot(lastSuccessfulScrapeInstant, lastScrapeFailureInstant, lastSuccessfulRecomputeInstant, when, false, unknownItemNames, failedAvatarNames);
		}

		private Snapshot withSuccessfulRecompute(Instant when, List<String> unknownItemNames, List<String> failedAvatarNames) {
			return new Snapshot(lastSuccessfulScrapeInstant, lastScrapeFailureInstant, when, lastRecomputeFailureInstant, true, unknownItemNames, failedAvatarNames);
		}
	}
}
