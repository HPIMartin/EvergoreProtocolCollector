package dev.schoenberg.evergore.protocolParser.entry;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public abstract class Entry {
	public final String avatar;
	public final Instant date;
	public final List<Item> items;

	public Entry(String avatar, Instant date, List<Item> items) {
		this.avatar = avatar;
		this.date = date;
		this.items = items;
	}

	protected abstract String getType();

	public String print() {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.of("Europe/Berlin"));
		String prefix = getType() + "\t" + formatter.format(date) + "\t" + avatar + "\t";
		List<String> result = new ArrayList<>();
		for (Item item : items) {
			result.add(prefix + item.toString("\t"));
		}
		result.forEach(System.out::println);
		return result.stream().reduce((x, y) -> x + "\n" + y).orElse("");
	}
}
