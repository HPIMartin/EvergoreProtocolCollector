package dev.schoenberg.evergore.protocolParser.monitoring;

import java.time.*;
import java.util.*;

import io.micronaut.health.*;
import io.micronaut.management.health.indicator.*;
import org.junit.jupiter.api.*;
import org.reactivestreams.*;

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
	void reportsUpWithTimestampDetailAfterSuccessfulRun() {
		Instant recorded = Instant.parse("2026-06-21T12:00:00Z");
		lastRunStatus.recordSuccessfulRun(recorded);

		HealthResult result = singleResult();

		assertThat(result.getStatus()).isEqualTo(HealthStatus.UP);
		assertThat(result.getDetails()).isInstanceOf(Map.class);
		@SuppressWarnings("unchecked")
		Map<String, Object> details = (Map<String, Object>) result.getDetails();
		assertThat(details).containsKey("lastSuccessfulRun");
		assertThat(details.get("lastSuccessfulRun")).isEqualTo(recorded.toString());
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
