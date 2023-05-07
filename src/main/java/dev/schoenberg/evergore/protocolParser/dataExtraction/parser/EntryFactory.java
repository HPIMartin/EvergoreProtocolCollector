package dev.schoenberg.evergore.protocolParser.dataExtraction.parser;

import static dev.schoenberg.evergore.protocolParser.businessLogic.Constants.*;

import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.regex.*;

import dev.schoenberg.evergore.protocolParser.domain.*;

public class EntryFactory {
	public static Entry parseContent(List<String> rawContent) {
		List<Item> items = parseItems(rawContent.subList(1, rawContent.size()));
		return generateEntry(rawContent.get(0), items);
	}

	private static Entry generateEntry(String headline, List<Item> items) {
		Pattern pattern = Pattern.compile(LAGER_EINTRAG_START);
		Matcher matcher = pattern.matcher(headline);
		matcher.find();
		String avatar = matcher.group(GROUP_NAME_AVATAR);
		DateTimeFormatter FMT = new DateTimeFormatterBuilder().appendPattern("dd.MM.yyyy HH:mm").toFormatter().withZone(APP_ZONE);
		Instant date = FMT.parse(matcher.group(GROUP_NAME_DATE), Instant::from);

		if ("Entnahme".equals(matcher.group(GROUP_NAME_TYPE))) {
			return new Withdrawal(avatar.trim(), date, items);
		}
		return new Storage(avatar.trim(), date, items);
	}

	private static List<Item> parseItems(List<String> rawItems) {
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
			if (!matcher.find()) {
				continue;
			}
			items.add(new Item(Integer.parseInt(matcher.group(amount)), matcher.group(itemName).trim(), parseQuality(matcher.group(itemQuality))));
		}

		return deduplicate(items);
	}

	private static List<Item> deduplicate(List<Item> items) {
		List<Item> filtered = new ArrayList<>();
		for (Item item : items) {
			Optional<Item> any = filtered.stream().filter(x -> x.name.equals(item.name) && x.quality == item.quality).findAny();
			if (any.isPresent()) {
				Item found = any.get();
				found.quantity = found.quantity + item.quantity;
			} else {
				filtered.add(item);
			}
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
