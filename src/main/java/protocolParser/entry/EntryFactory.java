package protocolParser.entry;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.List;

public class EntryFactory {
	public static Entry parseContent(List<String> rawContent) {
		List<Item> items = parseItems(rawContent.subList(1, rawContent.size()));
		return generateEntry(rawContent.get(0), items);

	}

	private static Entry generateEntry(String headline, List<Item> items) {
		String[] splitted = headline.split("\t");
		String avatar = splitted[1];
		DateTimeFormatter FMT = new DateTimeFormatterBuilder().appendPattern("dd.MM.yyyy HH:mm").toFormatter().withZone(ZoneId.of("Europe/Berlin"));
		Instant date = FMT.parse(splitted[0], Instant::from);

		if (splitted[2].equals("Entnahme")) {
			return new Withdrawal(avatar, date, items);
		} else {
			return new Storage(avatar, date, items);
		}
	}

	private static List<Item> parseItems(List<String> rawItems) {
		List<Item> items = new ArrayList<>();
		for (String item : rawItems) {
			if (!item.matches("\\d.*")) {
				continue;
			}
			String[] splitted = item.split(" ");
			items.add(new Item(Integer.valueOf(splitted[0]), splitted[1], parseQuality(splitted)));
		}
		return items;
	}

	private static int parseQuality(String[] splitted) {
		if (splitted.length > 2) {
			String value = splitted[2];
			if (value.startsWith("(")) {
				return Integer.valueOf(value.substring(1, value.length() - 1));
			}
		}
		return 100;
	}
}
