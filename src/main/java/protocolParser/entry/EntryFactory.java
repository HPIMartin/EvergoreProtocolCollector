package protocolParser.entry;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import protocolParser.Main;

public class EntryFactory {
	public static Entry parseContent(List<String> rawContent) {
		List<Item> items = parseItems(rawContent.subList(1, rawContent.size()));
		return generateEntry(rawContent.get(0), items);

	}

	private static Entry generateEntry(String headline, List<Item> items) {
		Pattern pattern = Pattern.compile(Main.LAGER_EINTRAG_START);
		Matcher matcher = pattern.matcher(headline);
		matcher.find();
		String avatar = matcher.group(Main.GROUP_NAME_AVATAR);
		DateTimeFormatter FMT = new DateTimeFormatterBuilder().appendPattern("dd.MM.yyyy HH:mm").toFormatter().withZone(ZoneId.of("Europe/Berlin"));
		Instant date = FMT.parse(matcher.group(Main.GROUP_NAME_DATE), Instant::from);

		if (matcher.group(Main.GROUP_NAME_TYPE).equals("Entnahme")) {
			return new Withdrawal(avatar, date, items);
		} else {
			return new Storage(avatar, date, items);
		}
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
			items.add(new Item(Integer.valueOf(matcher.group(amount)), matcher.group(itemName), parseQuality(matcher.group(itemQuality))));
		}

		items = dedupicate(items);

		return items;
	}

	private static List<Item> dedupicate(List<Item> items) {
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
		} else {
			return Integer.valueOf(quality);
		}
	}
}
