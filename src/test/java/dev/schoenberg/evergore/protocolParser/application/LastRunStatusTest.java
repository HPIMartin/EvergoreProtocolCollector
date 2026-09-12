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

	private LastRunStatus.Snapshot snapshot() {
		return tested.snapshot();
	}

	@Test
	void isEmptyInitiallyForScrapeAndRecomputeOutcomes() {
		LastRunStatus.Snapshot snapshot = snapshot();

		assertThat(snapshot.lastSuccessfulScrape()).isEmpty();
		assertThat(snapshot.lastScrapeFailure()).isEmpty();
		assertThat(snapshot.lastSuccessfulRecompute()).isEmpty();
		assertThat(snapshot.lastRecomputeFailure()).isEmpty();
	}

	@Test
	void recordsTheSuccessfulScrapeInstant() {
		Instant first = Instant.parse("2026-06-21T08:00:00Z");
		Instant second = Instant.parse("2026-06-21T10:00:00Z");

		tested.recordSuccessfulScrape(first);
		tested.recordSuccessfulScrape(second);

		assertThat(snapshot().lastSuccessfulScrape()).contains(second);
	}

	@Test
	void recordsTheScrapeFailureInstant() {
		Instant first = Instant.parse("2026-06-21T08:00:00Z");
		Instant second = Instant.parse("2026-06-21T10:00:00Z");

		tested.recordScrapeFailure(first);
		tested.recordScrapeFailure(second);

		assertThat(snapshot().lastScrapeFailure()).contains(second);
	}

	@Test
	void recordsTheSuccessfulRecomputeInstant() {
		Instant first = Instant.parse("2026-06-21T08:00:00Z");
		Instant second = Instant.parse("2026-06-21T10:00:00Z");

		tested.recordSuccessfulRecompute(first, List.of(), List.of(), List.of());
		tested.recordSuccessfulRecompute(second, List.of(), List.of(), List.of());

		assertThat(snapshot().lastSuccessfulRecompute()).contains(second);
	}

	@Test
	void recordsTheRecomputeFailureInstant() {
		Instant first = Instant.parse("2026-06-21T08:00:00Z");
		Instant second = Instant.parse("2026-06-21T10:00:00Z");

		tested.recordRecomputeFailure(first);
		tested.recordRecomputeFailure(second);

		assertThat(snapshot().lastRecomputeFailure()).contains(second);
	}

	@Test
	void scrapeAndRecomputeOutcomesAreRecordedIndependently() {
		Instant scrapeSuccess = Instant.parse("2026-06-21T08:00:00Z");
		Instant scrapeFailure = Instant.parse("2026-06-21T09:00:00Z");
		Instant recomputeSuccess = Instant.parse("2026-06-21T10:00:00Z");
		Instant recomputeFailure = Instant.parse("2026-06-21T11:00:00Z");

		tested.recordSuccessfulScrape(scrapeSuccess);
		tested.recordScrapeFailure(scrapeFailure);
		tested.recordSuccessfulRecompute(recomputeSuccess, List.of(), List.of(), List.of());
		tested.recordRecomputeFailure(recomputeFailure);

		LastRunStatus.Snapshot snapshot = snapshot();
		assertThat(snapshot.lastSuccessfulScrape()).contains(scrapeSuccess);
		assertThat(snapshot.lastScrapeFailure()).contains(scrapeFailure);
		assertThat(snapshot.lastSuccessfulRecompute()).contains(recomputeSuccess);
		assertThat(snapshot.lastRecomputeFailure()).contains(recomputeFailure);
	}

	@Test
	void isNotRecomputeHealthyInitially() {
		assertThat(snapshot().recomputeHealthy()).isFalse();
	}

	@Test
	void becomesRecomputeHealthyAfterASuccessfulRecompute() {
		tested.recordSuccessfulRecompute(Instant.parse("2026-06-21T08:00:00Z"), List.of(), List.of(), List.of());

		assertThat(snapshot().recomputeHealthy()).isTrue();
	}

	@Test
	void becomesRecomputeUnhealthyAfterARecomputeFailureFollowingASuccess() {
		tested.recordSuccessfulRecompute(Instant.parse("2026-06-21T08:00:00Z"), List.of(), List.of(), List.of());
		tested.recordRecomputeFailure(Instant.parse("2026-06-21T09:00:00Z"));

		assertThat(snapshot().recomputeHealthy()).isFalse();
	}

	@Test
	void becomesRecomputeHealthyAgainAfterASubsequentSuccessFollowingAFailure() {
		tested.recordSuccessfulRecompute(Instant.parse("2026-06-21T08:00:00Z"), List.of(), List.of(), List.of());
		tested.recordRecomputeFailure(Instant.parse("2026-06-21T09:00:00Z"));
		tested.recordSuccessfulRecompute(Instant.parse("2026-06-21T10:00:00Z"), List.of(), List.of(), List.of());

		assertThat(snapshot().recomputeHealthy()).isTrue();
	}

	@Test
	void hasNoUnknownItemNamesInitially() {
		assertThat(snapshot().unknownItemNames()).isEmpty();
	}

	@Test
	void recordsTheUnknownItemNamesOfTheLastRun() {
		tested.recordSuccessfulRecompute(Instant.parse("2026-06-21T08:00:00Z"), List.of("Unobtainium", "Unobtainium"), List.of(), List.of());

		assertThat(snapshot().unknownItemNames()).containsExactly("Unobtainium", "Unobtainium");
	}

	@Test
	void clearsThePreviousRunsUnknownItemAndFailedAvatarNamesOnACleanRecompute() {
		tested.recordSuccessfulRecompute(Instant.parse("2026-06-21T08:00:00Z"), List.of("Unobtainium"), List.of(), List.of("Zwerg"));

		tested.recordSuccessfulRecompute(Instant.parse("2026-06-21T09:00:00Z"), List.of(), List.of(), List.of());

		LastRunStatus.Snapshot snapshot = snapshot();
		assertThat(snapshot.unknownItemNames()).isEmpty();
		assertThat(snapshot.failedAvatarNames()).isEmpty();
	}

	@Test
	void recordsTheZeroValuedItemNamesOfTheLastRun() {
		tested.recordSuccessfulRecompute(Instant.parse("2026-06-21T08:00:00Z"), List.of(), List.of("Übungsstück-Sorandilaxt"), List.of());

		assertThat(snapshot().zeroValuedItemNames()).containsExactly("Übungsstück-Sorandilaxt");
	}

	@Test
	void clearsThePreviousRunsZeroValuedItemNamesOnACleanRecompute() {
		tested.recordSuccessfulRecompute(Instant.parse("2026-06-21T08:00:00Z"), List.of(), List.of("Übungsstück-Sorandilaxt"), List.of());

		tested.recordSuccessfulRecompute(Instant.parse("2026-06-21T09:00:00Z"), List.of(), List.of(), List.of());

		assertThat(snapshot().zeroValuedItemNames()).isEmpty();
	}

	@Test
	void keepsTheLastSuccessfulRunsZeroValuedItemNamesWhenTheNextRecomputeFails() {
		tested.recordSuccessfulRecompute(Instant.parse("2026-06-21T08:00:00Z"), List.of(), List.of("Übungsstück-Sorandilaxt"), List.of());

		tested.recordRecomputeFailure(Instant.parse("2026-06-21T09:00:00Z"));

		assertThat(snapshot().zeroValuedItemNames()).containsExactly("Übungsstück-Sorandilaxt");
	}

	@Test
	void keepsTheLastSuccessfulRunsInstantAndNamesWhenTheNextRecomputeFails() {
		tested.recordSuccessfulRecompute(Instant.parse("2026-06-21T08:00:00Z"), List.of("Unobtainium"), List.of(), List.of("Zwerg"));

		tested.recordRecomputeFailure(Instant.parse("2026-06-21T09:00:00Z"));

		LastRunStatus.Snapshot snapshot = snapshot();
		assertThat(snapshot.lastSuccessfulRecompute()).contains(Instant.parse("2026-06-21T08:00:00Z"));
		assertThat(snapshot.unknownItemNames()).containsExactly("Unobtainium");
		assertThat(snapshot.failedAvatarNames()).containsExactly("Zwerg");
		assertThat(snapshot.recomputeHealthy()).isFalse();
	}
}
