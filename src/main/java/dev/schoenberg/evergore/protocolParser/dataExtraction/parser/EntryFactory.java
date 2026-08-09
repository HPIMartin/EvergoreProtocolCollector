package dev.schoenberg.evergore.protocolParser.dataExtraction.parser;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dev.schoenberg.evergore.protocolParser.Logger;
import dev.schoenberg.evergore.protocolParser.businessLogic.base.TransferType;
import dev.schoenberg.evergore.protocolParser.domain.Entry;
import dev.schoenberg.evergore.protocolParser.domain.Item;

import static dev.schoenberg.evergore.protocolParser.businessLogic.Constants.APP_ZONE;
import static dev.schoenberg.evergore.protocolParser.businessLogic.Constants.GROUP_NAME_AVATAR;
import static dev.schoenberg.evergore.protocolParser.businessLogic.Constants.GROUP_NAME_DATE;
import static dev.schoenberg.evergore.protocolParser.businessLogic.Constants.GROUP_NAME_TYPE;
import static dev.schoenberg.evergore.protocolParser.businessLogic.Constants.LAGER_EINTRAG_START;

public class EntryFactory {
	// Stricter than LAGER_EINTRAG_BOUNDARY (literal dots) to tell an unknown type apart from a malformed date when warning.
	private static final Pattern TIMESTAMPED_HEADLINE = Pattern.compile("^\\d{2}\\.\\d{2}\\.\\d{4} \\d{2}:\\d{2}.*");

	private EntryFactory() {}

	public static Optional<Entry> parseContent(List<String> rawContent, Logger logger) {
		List<Item> items = parseItems(rawContent.subList(1, rawContent.size()), logger);
		return generateEntry(rawContent.get(0), items, logger);
	}

	private static Optional<Entry> generateEntry(String headline, List<Item> items, Logger logger) {
		Pattern pattern = Pattern.compile(LAGER_EINTRAG_START);
		Matcher matcher = pattern.matcher(headline);
		if (!matcher.find()) {
			if (TIMESTAMPED_HEADLINE.matcher(headline).matches()) {
				logger.warn("Dropping protocol entry: unmatched transfer type in headline: " + headline);
			}
			return Optional.empty();
		}
		String avatar = matcher.group(GROUP_NAME_AVATAR);
		DateTimeFormatter formatter = new DateTimeFormatterBuilder().appendPattern("dd.MM.yyyy HH:mm").toFormatter().withZone(APP_ZONE);
		Instant date;
		try {
			date = formatter.parse(matcher.group(GROUP_NAME_DATE), Instant::from);
		} catch (DateTimeParseException e) {
			logger.warn("Dropping protocol entry: out-of-range date in headline: " + headline);
			return Optional.empty();
		}

		if ("Entnahme".equals(matcher.group(GROUP_NAME_TYPE))) {
			return Optional.of(new Entry(avatar.trim(), date, items, TransferType.ENTNAHME));
		}
		return Optional.of(new Entry(avatar.trim(), date, items, TransferType.EINLAGERUNG));
	}

	private static List<Item> parseItems(List<String> rawItems, Logger logger) {
		String amount = "amount";
		String itemName = "name";
		String itemQuality = "quality";
		String itemRegex = "^(?<" + amount + ">\\d*) (?<" + itemName + ">[^\\(+]*)(\\((?<" + itemQuality + ">\\d*)\\))?\\s?(\\+1)?";

		// Matching entries:
		// 200 Heilsamer Seidenverband +1
		// 2 Kurzschwert (80) +1
		// 5 Schattenstaub

		Pattern pattern = Pattern.compile(itemRegex);

		List<Item> items = new ArrayList<>();
		for (String item : rawItems) {
			if ("Impressum".equals(item)) {
				break;
			}
			Matcher matcher = pattern.matcher(item);
			if (matcher.find()) {
				Optional<Item> parsed = toItem(matcher.group(amount), matcher.group(itemName), matcher.group(itemQuality));
				if (parsed.isEmpty()) {
					logger.warn("Skipping item line with an unparseable number: " + item);
				} else {
					items.add(parsed.get());
				}
			}
		}

		return deduplicate(items);
	}

	private static Optional<Item> toItem(String amount, String itemName, String itemQuality) {
		try {
			return Optional.of(new Item(Integer.parseInt(amount), itemName.trim(), parseQuality(itemQuality)));
		} catch (NumberFormatException e) {
			return Optional.empty();
		}
	}

	private static List<Item> deduplicate(List<Item> items) {
		List<Item> filtered = new ArrayList<>();
		for (Item item : items) {
			Optional<Item> any = filtered.stream().filter(x -> x.name().equals(item.name()) && x.quality() == item.quality()).findAny();
			Item toAdd;
			if (any.isPresent()) {
				Item found = any.get();
				toAdd = new Item(found.quantity() + item.quantity(), found.name(), found.quality());
				filtered.remove(found);
			} else {
				toAdd = item;
			}
			filtered.add(toAdd);
		}
		return filtered;
	}

	private static int parseQuality(String quality) {
		if (quality == null || quality.isEmpty()) {
			return 100;
		}
		return Integer.parseInt(quality);
	}
}
