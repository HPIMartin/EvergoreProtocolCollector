package dev.schoenberg.evergore.protocolParser.application;

import java.time.*;
import java.util.*;

import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.*;

class LastRunStatusTest {

	private LastRunStatus tested;

	@BeforeEach
	void setup() {
		tested = new LastRunStatus();
	}

	@Test
	void isEmptyInitiallyForScrapeAndRecomputeOutcomes() {
		assertThat(tested.lastSuccessfulScrape()).isEmpty();
		assertThat(tested.lastScrapeFailure()).isEmpty();
		assertThat(tested.lastSuccessfulRecompute()).isEmpty();
		assertThat(tested.lastRecomputeFailure()).isEmpty();
	}

	@Test
	void recordsTheRecordedInstantForEachScrapeAndRecomputeOutcome() {
		Instant first = Instant.parse("2026-06-21T08:00:00Z");
		Instant second = Instant.parse("2026-06-21T10:00:00Z");

		tested.recordSuccessfulScrape(first);
		tested.recordSuccessfulScrape(second);
		assertThat(tested.lastSuccessfulScrape()).contains(second);

		tested.recordScrapeFailure(first);
		tested.recordScrapeFailure(second);
		assertThat(tested.lastScrapeFailure()).contains(second);

		tested.recordSuccessfulRecompute(first);
		tested.recordSuccessfulRecompute(second);
		assertThat(tested.lastSuccessfulRecompute()).contains(second);

		tested.recordRecomputeFailure(first);
		tested.recordRecomputeFailure(second);
		assertThat(tested.lastRecomputeFailure()).contains(second);
	}

	@Test
	void scrapeAndRecomputeOutcomesAreRecordedIndependently() {
		Instant scrapeSuccess = Instant.parse("2026-06-21T08:00:00Z");
		Instant scrapeFailure = Instant.parse("2026-06-21T09:00:00Z");
		Instant recomputeSuccess = Instant.parse("2026-06-21T10:00:00Z");
		Instant recomputeFailure = Instant.parse("2026-06-21T11:00:00Z");

		tested.recordSuccessfulScrape(scrapeSuccess);
		tested.recordScrapeFailure(scrapeFailure);
		tested.recordSuccessfulRecompute(recomputeSuccess);
		tested.recordRecomputeFailure(recomputeFailure);

		assertThat(tested.lastSuccessfulScrape()).contains(scrapeSuccess);
		assertThat(tested.lastScrapeFailure()).contains(scrapeFailure);
		assertThat(tested.lastSuccessfulRecompute()).contains(recomputeSuccess);
		assertThat(tested.lastRecomputeFailure()).contains(recomputeFailure);
	}

	@Test
	void isNotRecomputeHealthyInitially() {
		assertThat(tested.recomputeHealthy()).isFalse();
	}

	@Test
	void becomesRecomputeHealthyAfterASuccessfulRecompute() {
		tested.recordSuccessfulRecompute(Instant.parse("2026-06-21T08:00:00Z"));

		assertThat(tested.recomputeHealthy()).isTrue();
	}

	@Test
	void becomesRecomputeUnhealthyAfterARecomputeFailureFollowingASuccess() {
		tested.recordSuccessfulRecompute(Instant.parse("2026-06-21T08:00:00Z"));
		tested.recordRecomputeFailure(Instant.parse("2026-06-21T09:00:00Z"));

		assertThat(tested.recomputeHealthy()).isFalse();
	}

	@Test
	void becomesRecomputeHealthyAgainAfterASubsequentSuccessFollowingAFailure() {
		tested.recordSuccessfulRecompute(Instant.parse("2026-06-21T08:00:00Z"));
		tested.recordRecomputeFailure(Instant.parse("2026-06-21T09:00:00Z"));
		tested.recordSuccessfulRecompute(Instant.parse("2026-06-21T10:00:00Z"));

		assertThat(tested.recomputeHealthy()).isTrue();
	}

	@Test
	void hasNoUnknownItemNamesInitially() {
		assertThat(tested.unknownItemNames()).isEmpty();
	}

	@Test
	void recordsTheUnknownItemNamesOfTheLastRun() {
		tested.recordUnknownItems(List.of("Unobtainium", "Unobtainium"));

		assertThat(tested.unknownItemNames()).containsExactly("Unobtainium", "Unobtainium");
	}
}
