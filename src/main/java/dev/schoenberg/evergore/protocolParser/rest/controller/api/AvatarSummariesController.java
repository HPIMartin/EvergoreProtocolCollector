package dev.schoenberg.evergore.protocolParser.rest.controller.api;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.validation.Validated;

import dev.schoenberg.evergore.protocolParser.Logger;
import dev.schoenberg.evergore.protocolParser.businessLogic.contribution.AvatarContribution;
import dev.schoenberg.evergore.protocolParser.businessLogic.contribution.AvatarContributions;
import dev.schoenberg.evergore.protocolParser.businessLogic.contribution.Contribution;
import dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationRepository;
import dev.schoenberg.evergore.protocolParser.rest.controller.api.wire.AvatarSummary;
import dev.schoenberg.evergore.protocolParser.rest.controller.api.wire.AvatarSummaryPage;
import dev.schoenberg.evergore.protocolParser.rest.controller.api.wire.GuildTotals;

import static dev.schoenberg.evergore.protocolParser.businessLogic.Constants.APP_ZONE;
import static dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey.getLastUpdatedKey;
import static dev.schoenberg.evergore.protocolParser.rest.controller.api.PageRequest.DEFAULT_PAGE;
import static dev.schoenberg.evergore.protocolParser.rest.controller.api.PageRequest.DEFAULT_SIZE;
import static dev.schoenberg.evergore.protocolParser.rest.controller.api.PageRequest.MAX_SIZE;
import static dev.schoenberg.evergore.protocolParser.rest.controller.api.PageRequest.PAGE;
import static dev.schoenberg.evergore.protocolParser.rest.controller.api.PageRequest.SIZE;
import static io.micronaut.http.MediaType.APPLICATION_JSON;

@Validated
@Controller(AvatarSummariesController.PATH)
public class AvatarSummariesController {
	public static final String PATH = "/api/v1/avatars";

	private final MetaInformationRepository metaRepo;
	private final AvatarContributions contributions;
	private final Logger logger;

	public AvatarSummariesController(MetaInformationRepository metaRepo, AvatarContributions contributions, Logger logger) {
		this.metaRepo = metaRepo;
		this.contributions = contributions;
		this.logger = logger;
	}

	@Get
	@Produces(APPLICATION_JSON)
	public AvatarSummaryPage summaries(@QueryValue(value = PAGE, defaultValue = DEFAULT_PAGE) @Min(0) int page,
			@QueryValue(value = SIZE, defaultValue = DEFAULT_SIZE) @Positive @Max(MAX_SIZE) int size) {
		PageRequest window = new PageRequest(page, size);
		List<AvatarContribution> guild = contributions.ofEveryKnownAvatar();

		logger.debug("Providing information for " + guild.size() + " avatars.");

		List<AvatarSummary> items = guild.stream().skip(window.offset()).limit(window.size()).map(AvatarSummariesController::summaryOf).toList();
		return new AvatarSummaryPage(lastUpdated(), window.page(), window.size(), guild.size(), totalsOf(guild), items);
	}

	private static GuildTotals totalsOf(List<AvatarContribution> guild) {
		Contribution total = Contribution.sumOf(guild.stream().map(avatar -> avatar.contribution().inWholeGold()).toList());

		return new GuildTotals(total.bankWithdrawn(), total.bankDeposited(), (long) total.storageWithdrawn(), (long) total.storageDeposited(), (long) total.net());
	}

	private static AvatarSummary summaryOf(AvatarContribution avatar) {
		Contribution whole = avatar.contribution().inWholeGold();

		return new AvatarSummary(avatar.avatar(), whole.bankWithdrawn(), whole.bankDeposited(), (long) whole.storageWithdrawn(), (long) whole.storageDeposited(),
				(long) whole.net(), avatar.lastBankActivity(), avatar.lastStorageActivity());
	}

	private Instant lastUpdated() {
		return metaRepo.get(getLastUpdatedKey()).map(this::toInstant).orElse(null);
	}

	private Instant toInstant(LocalDateTime lastUpdated) {
		return lastUpdated.atZone(APP_ZONE).toInstant();
	}
}
