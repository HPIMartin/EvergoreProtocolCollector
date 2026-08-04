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
import dev.schoenberg.evergore.protocolParser.businessLogic.banking.BankRepository;
import dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationRepository;
import dev.schoenberg.evergore.protocolParser.rest.controller.api.wire.AvatarSummary;
import dev.schoenberg.evergore.protocolParser.rest.controller.api.wire.AvatarSummaryPage;

import static dev.schoenberg.evergore.protocolParser.businessLogic.Constants.APP_ZONE;
import static dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey.getBankPlacement;
import static dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey.getBankWithdrawl;
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
	private final BankRepository bankRepo;
	private final Logger logger;

	public AvatarSummariesController(MetaInformationRepository metaRepo, BankRepository bankRepo, Logger logger) {
		this.metaRepo = metaRepo;
		this.bankRepo = bankRepo;
		this.logger = logger;
	}

	@Get
	@Produces(APPLICATION_JSON)
	public AvatarSummaryPage summaries(@QueryValue(value = PAGE, defaultValue = DEFAULT_PAGE) @Min(0) int page,
			@QueryValue(value = SIZE, defaultValue = DEFAULT_SIZE) @Positive @Max(MAX_SIZE) int size) {
		PageRequest window = new PageRequest(page, size);
		List<String> avatars = bankRepo.getAllDifferentAvatars().stream().sorted().toList();

		logger.debug("Providing information for " + avatars.size() + " avatars.");

		List<AvatarSummary> items = avatars.stream().skip(window.offset()).limit(window.size()).map(this::summaryOf).toList();
		return new AvatarSummaryPage(lastUpdated(), window.page(), window.size(), avatars.size(), items);
	}

	private AvatarSummary summaryOf(String avatar) {
		return new AvatarSummary(avatar, metaRepo.get(getBankWithdrawl(avatar)).orElse(0L), metaRepo.get(getBankPlacement(avatar)).orElse(0L));
	}

	private Instant lastUpdated() {
		return metaRepo.get(getLastUpdatedKey()).map(this::toInstant).orElse(null);
	}

	private Instant toInstant(LocalDateTime lastUpdated) {
		return lastUpdated.atZone(APP_ZONE).toInstant();
	}
}
