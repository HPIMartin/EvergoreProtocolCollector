package dev.schoenberg.evergore.protocolParser.acceptance.operator;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import dev.schoenberg.evergore.protocolParser.acceptance.browser.AdminPage;

import static dev.schoenberg.evergore.protocolParser.acceptance.world.GameProtocol.MINUTE;
import static dev.schoenberg.evergore.protocolParser.businessLogic.Constants.APP_ZONE;

public record AdminReport(AdminPage page) implements RunReport {
	private static final String LABEL_END = ": ";
	private static final String NAME_SEPARATOR = ", ";
	private static final Set<String> NOTHING_YET = Set.of("noch kein Scrape gelaufen", "noch keine Neuberechnung gelaufen");

	@Override
	public Optional<Instant> lastSuccessfulScrape() {
		return momentAfter("Letzter Scrape");
	}

	@Override
	public Optional<Instant> lastSuccessfulRecompute() {
		return momentAfter("Letzte Neuberechnung");
	}

	@Override
	public Optional<Instant> lastScrapeFailure() {
		return momentAfter("Letzter Scrape-Fehler");
	}

	@Override
	public Optional<Instant> lastRecomputeFailure() {
		return momentAfter("Letzter Fehler bei der Neuberechnung");
	}

	@Override
	public Optional<List<String>> list(FindingList list) {
		return list.heading().flatMap(this::textAfter).map(names -> Arrays.asList(names.split(NAME_SEPARATOR)));
	}

	private Optional<Instant> momentAfter(String label) {
		return textAfter(label).flatMap(AdminReport::momentIn);
	}

	private Optional<String> textAfter(String label) {
		return page.messages().stream().filter(line -> line.startsWith(label + LABEL_END)).findFirst().map(line -> line.substring(label.length() + LABEL_END.length()));
	}

	private static Optional<Instant> momentIn(String text) {
		if (NOTHING_YET.contains(text)) {
			return Optional.empty();
		}
		try {
			return Optional.of(LocalDateTime.parse(text, MINUTE).atZone(APP_ZONE).toInstant());
		} catch (DateTimeParseException e) {
			throw new AssertionError("The admin page shows a date that is no German wall-clock minute: " + text, e);
		}
	}
}
