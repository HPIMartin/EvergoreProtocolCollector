package dev.schoenberg.evergore.protocolParser.acceptance.operator;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface RunReport {
	Optional<Instant> lastSuccessfulScrape();

	Optional<Instant> lastSuccessfulRecompute();

	Optional<Instant> lastScrapeFailure();

	Optional<Instant> lastRecomputeFailure();

	Optional<List<String>> list(FindingList list);
}
