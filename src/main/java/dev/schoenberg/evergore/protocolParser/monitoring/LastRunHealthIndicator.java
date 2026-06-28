package dev.schoenberg.evergore.protocolParser.monitoring;

import java.util.*;

import jakarta.inject.*;

import io.micronaut.health.*;
import io.micronaut.management.health.indicator.*;
import org.reactivestreams.*;

import dev.schoenberg.evergore.protocolParser.application.*;

@Singleton
public class LastRunHealthIndicator implements HealthIndicator {

	private static final String NAME = "lastRun";

	private final LastRunStatus lastRunStatus;

	public LastRunHealthIndicator(LastRunStatus lastRunStatus) {
		this.lastRunStatus = lastRunStatus;
	}

	@Override
	public Publisher<HealthResult> getResult() {
		return subscriber -> {
			subscriber.onSubscribe(new Subscription() {
				@Override
				public void request(long n) {
					subscriber.onNext(buildResult());
					subscriber.onComplete();
				}

				@Override
				public void cancel() {}
			});
		};
	}

	private HealthResult buildResult() {
		return lastRunStatus
				.lastSuccessfulRun()
				.map(instant -> HealthResult.builder(NAME, HealthStatus.UP).details(Map.of("lastSuccessfulRun", instant.toString())).build())
				.orElseGet(() -> HealthResult.builder(NAME, HealthStatus.UNKNOWN).build());
	}
}
