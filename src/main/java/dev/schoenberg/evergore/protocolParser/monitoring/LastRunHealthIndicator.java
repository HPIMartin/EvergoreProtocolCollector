package dev.schoenberg.evergore.protocolParser.monitoring;

import java.time.*;
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
				.map(instant -> HealthResult.builder(NAME, HealthStatus.UP).details(details(instant)).build())
				.orElseGet(() -> HealthResult.builder(NAME, HealthStatus.UNKNOWN).build());
	}

	private Map<String, Object> details(Instant instant) {
		Map<String, Object> details = new HashMap<>();
		details.put("lastSuccessfulRun", instant.toString());

		List<String> unknownItemNames = lastRunStatus.unknownItemNames();
		if (!unknownItemNames.isEmpty()) {
			details.put("unknownItemCount", unknownItemNames.size());
			details.put("unknownItemNames", unknownItemNames.stream().distinct().sorted().toList());
		}

		List<String> failedAvatarNames = lastRunStatus.failedAvatarNames();
		if (!failedAvatarNames.isEmpty()) {
			details.put("failedAvatarCount", failedAvatarNames.size());
			details.put("failedAvatarNames", failedAvatarNames.stream().distinct().sorted().toList());
		}

		return details;
	}
}
