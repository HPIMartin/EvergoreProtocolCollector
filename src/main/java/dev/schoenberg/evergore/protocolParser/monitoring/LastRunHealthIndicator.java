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
		LastRunStatus.Snapshot snapshot = lastRunStatus.snapshot();
		boolean recomputeAttempted = snapshot.lastSuccessfulRecompute().isPresent() || snapshot.lastRecomputeFailure().isPresent();
		if (!recomputeAttempted) {
			return HealthResult.builder(NAME, HealthStatus.UNKNOWN).build();
		}
		HealthStatus status = snapshot.recomputeHealthy() ? HealthStatus.UP : HealthStatus.DOWN;
		return HealthResult.builder(NAME, status).details(details(snapshot)).build();
	}

	private Map<String, Object> details(LastRunStatus.Snapshot snapshot) {
		Map<String, Object> details = new HashMap<>();
		snapshot.lastSuccessfulRecompute().ifPresent(instant -> details.put("lastSuccessfulRecompute", instant.toString()));

		snapshot.lastSuccessfulScrape().ifPresent(scrape -> details.put("lastSuccessfulScrape", scrape.toString()));
		snapshot.lastScrapeFailure().ifPresent(failure -> details.put("lastScrapeFailure", failure.toString()));
		snapshot.lastRecomputeFailure().ifPresent(failure -> details.put("lastRecomputeFailure", failure.toString()));

		List<String> unknownItemNames = snapshot.unknownItemNames();
		if (!unknownItemNames.isEmpty()) {
			details.put("unknownItemCount", unknownItemNames.size());
			details.put("unknownItemNames", unknownItemNames.stream().distinct().sorted().toList());
		}

		List<String> failedAvatarNames = snapshot.failedAvatarNames();
		if (!failedAvatarNames.isEmpty()) {
			details.put("failedAvatarCount", failedAvatarNames.size());
			details.put("failedAvatarNames", failedAvatarNames.stream().distinct().sorted().toList());
		}

		return details;
	}
}
