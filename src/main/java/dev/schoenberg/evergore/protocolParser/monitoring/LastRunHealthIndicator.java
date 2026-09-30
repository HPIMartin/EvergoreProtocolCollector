package dev.schoenberg.evergore.protocolParser.monitoring;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.inject.Singleton;

import io.micronaut.health.HealthStatus;
import io.micronaut.management.health.indicator.HealthIndicator;
import io.micronaut.management.health.indicator.HealthResult;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;

import dev.schoenberg.evergore.protocolParser.application.LastRunStatus;
import dev.schoenberg.evergore.protocolParser.businessLogic.GermanOrder;
import dev.schoenberg.evergore.protocolParser.businessLogic.roundTrip.RoundTrip;
import dev.schoenberg.evergore.protocolParser.businessLogic.roundTrip.RoundTripAbstention;
import dev.schoenberg.evergore.protocolParser.businessLogic.roundTrip.RoundTripOrder;

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

		putNames(details, "unknownItemCount", "unknownItemNames", snapshot.unknownItemNames());
		putNames(details, "zeroValuedItemCount", "zeroValuedItemNames", snapshot.zeroValuedItemNames());
		putNames(details, "failedAvatarCount", "failedAvatarNames", snapshot.failedAvatarNames());

		List<RoundTrip> roundTrips = snapshot.roundTrips();
		if (!roundTrips.isEmpty()) {
			details.put("roundTripCount", roundTrips.size());
			details.put("roundTrips", roundTrips.stream().sorted(RoundTripOrder.TRIPS).map(LastRunHealthIndicator::describe).toList());
		}
		List<RoundTripAbstention> roundTripAbstentions = snapshot.roundTripAbstentions();
		if (!roundTripAbstentions.isEmpty()) {
			details.put("roundTripAbstentionCount", roundTripAbstentions.size());
			details.put("roundTripAbstentions", roundTripAbstentions.stream().sorted(RoundTripOrder.ABSTENTIONS).map(LastRunHealthIndicator::describe).toList());
		}

		return details;
	}

	private static void putNames(Map<String, Object> details, String countKey, String namesKey, List<String> recorded) {
		List<String> names = GermanOrder.distinctSorted(recorded);
		if (!names.isEmpty()) {
			details.put(countKey, names.size());
			details.put(namesKey, names);
		}
	}

	private static String describe(RoundTrip roundTrip) {
		return roundTrip.avatar() + ": " + roundTrip.quantity() + " × " + roundTrip.item().ingameName;
	}

	private static String describe(RoundTripAbstention abstention) {
		return abstention.avatar() + ": " + abstention.item().ingameName;
	}
}
