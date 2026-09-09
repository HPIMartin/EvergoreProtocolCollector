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
		LastRunStatus.Snapshot snapshot = tested.snapshot();

		assertThat(snapshot.lastSuccessfulScrape()).isEmpty();
		assertThat(snapshot.lastScrapeFailure()).isEmpty();
		assertThat(snapshot.lastSuccessfulRecompute()).isEmpty();
		assertThat(snapshot.lastRecomputeFailure()).isEmpty();
	}

	@Test
	void recordsTheRecordedInstantForEachScrapeAndRecomputeOutcome() {
		Instant first = Instant.parse("2026-06-21T08:00:00Z");
		Instant second = Instant.parse("2026-06-21T10:00:00Z");

		tested.recordSuccessfulScrape(first);
		tested.recordSuccessfulScrape(second);
		assertThat(tested.snapshot().lastSuccessfulScrape()).contains(second);

		tested.recordScrapeFailure(first);
		tested.recordScrapeFailure(second);
		assertThat(tested.snapshot().lastScrapeFailure()).contains(second);

		tested.recordSuccessfulRecompute(first, List.of(), List.of());
		tested.recordSuccessfulRecompute(second, List.of(), List.of());
		assertThat(tested.snapshot().lastSuccessfulRecompute()).contains(second);

		tested.recordRecomputeFailure(first);
		tested.recordRecomputeFailure(second);
		assertThat(tested.snapshot().lastRecomputeFailure()).contains(second);
	}

	@Test
	void scrapeAndRecomputeOutcomesAreRecordedIndependently() {
		Instant scrapeSuccess = Instant.parse("2026-06-21T08:00:00Z");
		Instant scrapeFailure = Instant.parse("2026-06-21T09:00:00Z");
		Instant recomputeSuccess = Instant.parse("2026-06-21T10:00:00Z");
		Instant recomputeFailure = Instant.parse("2026-06-21T11:00:00Z");

		tested.recordSuccessfulScrape(scrapeSuccess);
		tested.recordScrapeFailure(scrapeFailure);
		tested.recordSuccessfulRecompute(recomputeSuccess, List.of(), List.of());
		tested.recordRecomputeFailure(recomputeFailure);

		LastRunStatus.Snapshot snapshot = tested.snapshot();
		assertThat(snapshot.lastSuccessfulScrape()).contains(scrapeSuccess);
		assertThat(snapshot.lastScrapeFailure()).contains(scrapeFailure);
		assertThat(snapshot.lastSuccessfulRecompute()).contains(recomputeSuccess);
		assertThat(snapshot.lastRecomputeFailure()).contains(recomputeFailure);
	}

	@Test
	void isNotRecomputeHealthyInitially() {
		assertThat(tested.snapshot().recomputeHealthy()).isFalse();
	}

	@Test
	void becomesRecomputeHealthyAfterASuccessfulRecompute() {
		tested.recordSuccessfulRecompute(Instant.parse("2026-06-21T08:00:00Z"), List.of(), List.of());

		assertThat(tested.snapshot().recomputeHealthy()).isTrue();
	}

	@Test
	void becomesRecomputeUnhealthyAfterARecomputeFailureFollowingASuccess() {
		tested.recordSuccessfulRecompute(Instant.parse("2026-06-21T08:00:00Z"), List.of(), List.of());
		tested.recordRecomputeFailure(Instant.parse("2026-06-21T09:00:00Z"));

		assertThat(tested.snapshot().recomputeHealthy()).isFalse();
	}

	@Test
	void becomesRecomputeHealthyAgainAfterASubsequentSuccessFollowingAFailure() {
		tested.recordSuccessfulRecompute(Instant.parse("2026-06-21T08:00:00Z"), List.of(), List.of());
		tested.recordRecomputeFailure(Instant.parse("2026-06-21T09:00:00Z"));
		tested.recordSuccessfulRecompute(Instant.parse("2026-06-21T10:00:00Z"), List.of(), List.of());

		assertThat(tested.snapshot().recomputeHealthy()).isTrue();
	}

	@Test
	void hasNoUnknownItemNamesInitially() {
		assertThat(tested.snapshot().unknownItemNames()).isEmpty();
	}

	@Test
	void recordsTheUnknownItemNamesOfTheLastRun() {
		tested.recordSuccessfulRecompute(Instant.parse("2026-06-21T08:00:00Z"), List.of("Unobtainium", "Unobtainium"), List.of());

		assertThat(tested.snapshot().unknownItemNames()).containsExactly("Unobtainium", "Unobtainium");
	}

	@Test
	void clearsThePreviousRunsUnknownItemAndFailedAvatarNamesOnACleanRecompute() {
		tested.recordSuccessfulRecompute(Instant.parse("2026-06-21T08:00:00Z"), List.of("Unobtainium"), List.of("Zwerg"));

		tested.recordSuccessfulRecompute(Instant.parse("2026-06-21T09:00:00Z"), List.of(), List.of());

		LastRunStatus.Snapshot snapshot = tested.snapshot();
		assertThat(snapshot.unknownItemNames()).isEmpty();
		assertThat(snapshot.failedAvatarNames()).isEmpty();
	}

	@Test
	void keepsTheLastSuccessfulRunsInstantAndNamesWhenTheNextRecomputeFails() {
		tested.recordSuccessfulRecompute(Instant.parse("2026-06-21T08:00:00Z"), List.of("Unobtainium"), List.of("Zwerg"));

		tested.recordRecomputeFailure(Instant.parse("2026-06-21T09:00:00Z"));

		LastRunStatus.Snapshot snapshot = tested.snapshot();
		assertThat(snapshot.lastSuccessfulRecompute()).contains(Instant.parse("2026-06-21T08:00:00Z"));
		assertThat(snapshot.unknownItemNames()).containsExactly("Unobtainium");
		assertThat(snapshot.failedAvatarNames()).containsExactly("Zwerg");
		assertThat(snapshot.recomputeHealthy()).isFalse();
	}
}
