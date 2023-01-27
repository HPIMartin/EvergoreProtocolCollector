package dev.schoenberg.evergore.protocolParser.domain;

import java.time.*;
import java.time.format.*;
import java.util.*;

public abstract class Entry {
	public final String avatar;
	public final Instant date;
	public final List<Item> items;

	public Entry(String avatar, Instant date, List<Item> items) {
		this.avatar = avatar;
		this.date = date;
		this.items = items;
	}

	public abstract String getType();

	public String print(String delimiter) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
				.withZone(ZoneId.of("Europe/Berlin"));
		String prefix = getType() + delimiter + formatter.format(date) + delimiter + avatar + delimiter;
		List<String> result = new ArrayList<>();
		for (Item item : items) {
			result.add(prefix + item.toString(delimiter));
		}
		return result.stream().reduce((x, y) -> x + "\n" + y).orElse("");
	}
}
