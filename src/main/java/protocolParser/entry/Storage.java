package protocolParser.entry;

import java.time.Instant;
import java.util.List;

public class Storage extends Entry {
	public Storage(String avatar, Instant date, List<Item> items) {
		super(avatar, date, items);
	}

	@Override
	protected String getType() {
		return "Einlagerung";
	}
}
