package dev.schoenberg.evergore.protocolParser.rest.controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Produces;

import dev.schoenberg.evergore.protocolParser.Logger;
import dev.schoenberg.evergore.protocolParser.businessLogic.banking.BankRepository;
import dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationRepository;
import dev.schoenberg.evergore.protocolParser.rest.controller.OutputFormatter.Column;

import static dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey.getBankPlacement;
import static dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey.getBankWithdrawl;
import static dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey.getLastUpdatedKey;
import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.silentThrow;
import static io.micronaut.http.MediaType.TEXT_HTML;
import static java.lang.String.valueOf;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.format.DateTimeFormatter.ofPattern;

@Controller("/overview")
public class OverviewController {
	private static final DateTimeFormatter DATE_TIME_PATTERN = ofPattern("dd.MM.yyyy HH:mm");
	private final MetaInformationRepository metaRepo;
	private final BankRepository bankRepo;
	private final OutputFormatter formatter;
	private final Logger logger;

	public OverviewController(MetaInformationRepository metaRepo, BankRepository bankRepo, OutputFormatter formatter, Logger logger) {
		this.metaRepo = metaRepo;
		this.bankRepo = bankRepo;
		this.formatter = formatter;
		this.logger = logger;
	}

	@Get("/")
	@Produces(TEXT_HTML)
	public String metaInformation() {
		LocalDateTime lastUpdated = metaRepo.get(getLastUpdatedKey()).orElse(LocalDateTime.MIN);
		String pageWithMeta = getPageTemplateWithMetaData(lastUpdated);

		List<BankInformation> all = getBankingInformation();

		logger.debug("Providing information for " + all.size() + " avatars.");

		List<Column<BankInformation>> columns = new ArrayList<>();
		columns.add(new Column<>("Avatar", b -> b.avatar));
		columns.add(new Column<>("Entnommen", b -> valueOf(b.withdrawl)));
		columns.add(new Column<>("Eingelagert", b -> valueOf(b.placement)));

		return pageWithMeta.replace("###PAGE_CONTENT_PLACEHOLDER###", formatter.createTable(all, columns));
	}

	private List<BankInformation> getBankingInformation() {
		return bankRepo.getAllDifferentAvatars().stream().sorted().map(this::getBankingInformation).toList();
	}

	private BankInformation getBankingInformation(String avatar) {
		long withdrawl = metaRepo.get(getBankWithdrawl(avatar)).orElse(0L);
		long placement = metaRepo.get(getBankPlacement(avatar)).orElse(0L);
		return new BankInformation(avatar, withdrawl, placement);
	}

	private String getPageTemplateWithMetaData(LocalDateTime lastUpdated) {
		String pageTemplate = new String(silentThrow(() -> getClass().getResourceAsStream("/static/overviewHtmlTemplate.txt").readAllBytes()), UTF_8);
		return pageTemplate.replace("###LAST_UPDATED_PLACEHOLDER###", lastUpdated.format(DATE_TIME_PATTERN));
	}

	private static record BankInformation(String avatar, long withdrawl, long placement) {}
}
