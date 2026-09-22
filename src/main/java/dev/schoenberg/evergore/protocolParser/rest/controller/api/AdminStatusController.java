package dev.schoenberg.evergore.protocolParser.rest.controller.api;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Produces;

import dev.schoenberg.evergore.protocolParser.application.LastRunStatus;
import dev.schoenberg.evergore.protocolParser.businessLogic.contribution.AvatarContributions;
import dev.schoenberg.evergore.protocolParser.businessLogic.contribution.GuildContributions;
import dev.schoenberg.evergore.protocolParser.businessLogic.roundTrip.RoundTrip;
import dev.schoenberg.evergore.protocolParser.businessLogic.roundTrip.RoundTripAbstention;
import dev.schoenberg.evergore.protocolParser.rest.controller.api.wire.AdminStatus;
import dev.schoenberg.evergore.protocolParser.rest.controller.api.wire.RoundTripAbstentionWire;
import dev.schoenberg.evergore.protocolParser.rest.controller.api.wire.RoundTripWire;

import static dev.schoenberg.evergore.protocolParser.businessLogic.Constants.APP_ZONE;
import static io.micronaut.http.MediaType.APPLICATION_JSON;

@Controller(AdminStatusController.PATH)
public class AdminStatusController {
	public static final String PATH = "/api/v1/admin/status";

	private final AvatarContributions contributions;
	private final LastRunStatus lastRunStatus;

	public AdminStatusController(AvatarContributions contributions, LastRunStatus lastRunStatus) {
		this.contributions = contributions;
		this.lastRunStatus = lastRunStatus;
	}

	@Get
	@Produces(APPLICATION_JSON)
	public AdminStatus status() {
		GuildContributions recompute = contributions.ofEveryKnownAvatar();
		LastRunStatus.Snapshot snapshot = lastRunStatus.snapshot();

		return new AdminStatus(lastUpdated(recompute), snapshot.lastSuccessfulScrape().orElse(null), snapshot.lastScrapeFailure().orElse(null),
				snapshot.lastSuccessfulRecompute().orElse(null), snapshot.lastRecomputeFailure().orElse(null), distinctlySorted(snapshot.unknownItemNames()),
				distinctlySorted(snapshot.failedAvatarNames()), roundTripsOf(snapshot), roundTripAbstentionsOf(snapshot));
	}

	private static List<String> distinctlySorted(List<String> names) {
		return names.stream().distinct().sorted().toList();
	}

	private static List<RoundTripWire> roundTripsOf(LastRunStatus.Snapshot snapshot) {
		return snapshot
				.roundTrips()
				.stream()
				.sorted(Comparator.comparing(RoundTrip::avatar).thenComparing(roundTrip -> roundTrip.item().ingameName))
				.map(roundTrip -> new RoundTripWire(roundTrip.avatar(), roundTrip.item().ingameName, roundTrip.quantity()))
				.toList();
	}

	private static List<RoundTripAbstentionWire> roundTripAbstentionsOf(LastRunStatus.Snapshot snapshot) {
		return snapshot
				.roundTripAbstentions()
				.stream()
				.sorted(Comparator.comparing(RoundTripAbstention::avatar).thenComparing(abstention -> abstention.item().ingameName))
				.map(abstention -> new RoundTripAbstentionWire(abstention.avatar(), abstention.item().ingameName))
				.toList();
	}

	private static Instant lastUpdated(GuildContributions recompute) {
		return recompute.lastUpdated().map(AdminStatusController::toInstant).orElse(null);
	}

	private static Instant toInstant(LocalDateTime lastUpdated) {
		return lastUpdated.atZone(APP_ZONE).toInstant();
	}
}
