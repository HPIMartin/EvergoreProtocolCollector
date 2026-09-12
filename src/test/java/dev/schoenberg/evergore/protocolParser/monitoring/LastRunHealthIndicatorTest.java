package dev.schoenberg.evergore.protocolParser.monitoring;

import java.time.*;
import java.util.*;

import io.micronaut.health.*;
import io.micronaut.management.health.indicator.*;
import org.junit.jupiter.api.*;
import org.reactivestreams.*;

import dev.schoenberg.evergore.protocolParser.application.*;

import static org.assertj.core.api.Assertions.*;

class LastRunHealthIndicatorTest {

	private LastRunStatus lastRunStatus;
	private LastRunHealthIndicator tested;

	@BeforeEach
	void setup() {
		lastRunStatus = new LastRunStatus();
		tested = new LastRunHealthIndicator(lastRunStatus);
	}

	@Test
	void reportsUnknownWhenNoRunHasHappened() {
		HealthResult result = singleResult();

		assertThat(result.getStatus()).isEqualTo(HealthStatus.UNKNOWN);
		assertThat(result.getDetails()).isNull();
	}

	@Test
	void reportsUpWithTimestampDetailAfterSuccessfulRecompute() {
		Instant recorded = Instant.parse("2026-06-21T12:00:00Z");
		lastRunStatus.recordSuccessfulRecompute(recorded, List.of(), List.of(), List.of());

		HealthResult result = singleResult();

		assertThat(result.getStatus()).isEqualTo(HealthStatus.UP);
		assertThat(result.getDetails()).isInstanceOf(Map.class);
		@SuppressWarnings("unchecked")
		Map<String, Object> details = (Map<String, Object>) result.getDetails();
		assertThat(details).containsKey("lastSuccessfulRecompute");
		assertThat(details.get("lastSuccessfulRecompute")).isEqualTo(recorded.toString());
	}

	@Test
	void reportsItemsKnownToBeWorthNothingApartFromUnknownOnes() {
		lastRunStatus
				.recordSuccessfulRecompute(Instant.parse("2026-06-21T12:00:00Z"), List.of("Unobtainium"), List.of("Übungsstück-Sorandilaxt", "Übungsstück-Sorandilaxt"), List.of());

		HealthResult result = singleResult();

		@SuppressWarnings("unchecked")
		Map<String, Object> details = (Map<String, Object>) result.getDetails();
		assertThat(details.get("unknownItemCount")).isEqualTo(1);
		assertThat(details.get("unknownItemNames")).isEqualTo(List.of("Unobtainium"));
		assertThat(details.get("zeroValuedItemCount")).isEqualTo(2);
		assertThat(details.get("zeroValuedItemNames")).isEqualTo(List.of("Übungsstück-Sorandilaxt"));
	}

	@Test
	void omitsTheZeroValuedItemDetailWhenNoneOccurredInLastRun() {
		lastRunStatus.recordSuccessfulRecompute(Instant.parse("2026-06-21T12:00:00Z"), List.of(), List.of(), List.of());

		HealthResult result = singleResult();

		@SuppressWarnings("unchecked")
		Map<String, Object> details = (Map<String, Object>) result.getDetails();
		assertThat(details).doesNotContainKey("zeroValuedItemCount");
	}

	@Test
	void omitsUnknownItemDetailWhenNoneOccurredInLastRun() {
		lastRunStatus.recordSuccessfulRecompute(Instant.parse("2026-06-21T12:00:00Z"), List.of(), List.of(), List.of());

		HealthResult result = singleResult();

		@SuppressWarnings("unchecked")
		Map<String, Object> details = (Map<String, Object>) result.getDetails();
		assertThat(details).doesNotContainKey("unknownItemCount");
	}

	@Test
	void reportsUnknownItemCountAndNamesWhenPresentInLastRun() {
		lastRunStatus.recordSuccessfulRecompute(Instant.parse("2026-06-21T12:00:00Z"), List.of("Unobtainium", "Unobtainium"), List.of(), List.of());

		HealthResult result = singleResult();

		@SuppressWarnings("unchecked")
		Map<String, Object> details = (Map<String, Object>) result.getDetails();
		assertThat(details).containsEntry("unknownItemCount", 2);
		assertThat(details).containsEntry("unknownItemNames", List.of("Unobtainium"));
	}

	@Test
	void reportsScrapeFailureButNotRecomputeFailureAfterASuccessfulRecomputeFollowedByAFailedScrape() {
		lastRunStatus.recordSuccessfulRecompute(Instant.parse("2026-06-21T12:00:00Z"), List.of(), List.of(), List.of());
		lastRunStatus.recordScrapeFailure(Instant.parse("2026-06-22T12:00:00Z"));

		HealthResult result = singleResult();

		assertThat(result.getStatus()).isEqualTo(HealthStatus.UP);
		@SuppressWarnings("unchecked")
		Map<String, Object> details = (Map<String, Object>) result.getDetails();
		assertThat(details).containsKey("lastScrapeFailure");
		assertThat(details).doesNotContainKey("lastRecomputeFailure");
	}

	@Test
	void reportsDownWithRecomputeFailureAfterASuccessfulRecomputeFollowedByAFailedRecompute() {
		lastRunStatus.recordSuccessfulRecompute(Instant.parse("2026-06-21T12:00:00Z"), List.of(), List.of(), List.of());
		lastRunStatus.recordRecomputeFailure(Instant.parse("2026-06-22T12:00:00Z"));

		HealthResult result = singleResult();

		assertThat(result.getStatus()).isEqualTo(HealthStatus.DOWN);
		@SuppressWarnings("unchecked")
		Map<String, Object> details = (Map<String, Object>) result.getDetails();
		assertThat(details).containsKey("lastRecomputeFailure");
		assertThat(details).doesNotContainKey("lastScrapeFailure");
	}

	@Test
	void reportsDownWithOnlyTheFailureDetailWhenTheFirstEverRecomputeAttemptFails() {
		lastRunStatus.recordRecomputeFailure(Instant.parse("2026-06-22T12:00:00Z"));

		HealthResult result = singleResult();

		assertThat(result.getStatus()).isEqualTo(HealthStatus.DOWN);
		@SuppressWarnings("unchecked")
		Map<String, Object> details = (Map<String, Object>) result.getDetails();
		assertThat(details).containsKey("lastRecomputeFailure");
		assertThat(details).doesNotContainKey("lastSuccessfulRecompute");
	}

	private HealthResult singleResult() {
		Publisher<HealthResult> publisher = tested.getResult();
		HealthResult[] holder = new HealthResult[1];
		publisher.subscribe(new Subscriber<HealthResult>() {
			@Override
			public void onSubscribe(Subscription s) {
				s.request(1);
			}

			@Override
			public void onNext(HealthResult healthResult) {
				holder[0] = healthResult;
			}

			@Override
			public void onError(Throwable t) {
				throw new RuntimeException(t);
			}

			@Override
			public void onComplete() {}
		});
		return holder[0];
	}
}
