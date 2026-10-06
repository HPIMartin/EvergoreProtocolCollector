package dev.schoenberg.evergore.protocolParser.rest.controller.api;

import java.util.List;
import java.util.Optional;
import java.util.function.ToLongFunction;

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
import dev.schoenberg.evergore.protocolParser.businessLogic.contribution.GuildContributions;
import dev.schoenberg.evergore.protocolParser.businessLogic.contribution.WholeGoldContribution;
import dev.schoenberg.evergore.protocolParser.businessLogic.contribution.WholeGoldShare;
import dev.schoenberg.evergore.protocolParser.rest.controller.api.wire.AvatarSummary;
import dev.schoenberg.evergore.protocolParser.rest.controller.api.wire.AvatarSummaryPage;
import dev.schoenberg.evergore.protocolParser.rest.controller.api.wire.GuildTotals;

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

	private final AvatarContributions contributions;
	private final Logger logger;

	public AvatarSummariesController(AvatarContributions contributions, Logger logger) {
		this.contributions = contributions;
		this.logger = logger;
	}

	@Get
	@Produces(APPLICATION_JSON)
	public AvatarSummaryPage summaries(@QueryValue(value = PAGE, defaultValue = DEFAULT_PAGE) @Min(0) int page,
			@QueryValue(value = SIZE, defaultValue = DEFAULT_SIZE) @Positive @Max(MAX_SIZE) int size) {
		PageRequest window = new PageRequest(page, size);
		GuildContributions recompute = contributions.ofEveryKnownAvatar();
		List<AvatarContribution> guild = recompute.avatars();

		logger.debug("Providing information for " + guild.size() + " avatars.");

		List<AvatarSummary> items = guild.stream().skip(window.offset()).limit(window.size()).map(AvatarSummariesController::summaryOf).toList();
		return new AvatarSummaryPage(window.page(), window.size(), guild.size(), totalsOf(recompute), items);
	}

	private static GuildTotals totalsOf(GuildContributions recompute) {
		Optional<WholeGoldContribution> total = recompute.total().map(Contribution::inWholeGold);

		return new GuildTotals(figureOf(total, WholeGoldContribution::bankWithdrawn), figureOf(total, WholeGoldContribution::bankDeposited),
				figureOf(total, WholeGoldContribution::storageWithdrawn), figureOf(total, WholeGoldContribution::storageDeposited), figureOf(total, WholeGoldContribution::net),
				shareOf(total, WholeGoldShare::donation), shareOf(total, WholeGoldShare::craftSubsidy), shareOf(total, WholeGoldShare::balance),
				shareOf(total, WholeGoldShare::storageValue), recompute.containsStaleSums());
	}

	private static AvatarSummary summaryOf(AvatarContribution avatar) {
		Optional<WholeGoldContribution> whole = avatar.contribution().map(Contribution::inWholeGold);

		return new AvatarSummary(avatar.avatar(), figureOf(whole, WholeGoldContribution::bankWithdrawn), figureOf(whole, WholeGoldContribution::bankDeposited),
				figureOf(whole, WholeGoldContribution::storageWithdrawn), figureOf(whole, WholeGoldContribution::storageDeposited), figureOf(whole, WholeGoldContribution::net),
				shareOf(whole, WholeGoldShare::donation), shareOf(whole, WholeGoldShare::craftSubsidy), shareOf(whole, WholeGoldShare::balance), avatar.lastBankActivity(),
				avatar.lastStorageActivity(), avatar.staleSumsFrom());
	}

	private static Long figureOf(Optional<WholeGoldContribution> contribution, ToLongFunction<WholeGoldContribution> figure) {
		return contribution.map(whole -> figure.applyAsLong(whole)).orElse(null);
	}

	private static Long shareOf(Optional<WholeGoldContribution> contribution, ToLongFunction<WholeGoldShare> figure) {
		return contribution.flatMap(WholeGoldContribution::guildShare).map(share -> figure.applyAsLong(share)).orElse(null);
	}
}
