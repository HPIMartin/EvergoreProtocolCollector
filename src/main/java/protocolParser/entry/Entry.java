package protocolParser.entry;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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

	public void print() {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.of("Europe/Berlin"));
		String prefix = getType() + "\t" + formatter.format(date) + "\t" + avatar + "\t";
		for (Item item : items) {
			System.out.println(prefix + item.toString("\t"));
		}
	}
}
