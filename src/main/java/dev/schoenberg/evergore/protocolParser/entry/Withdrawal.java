package dev.schoenberg.evergore.protocolParser.entry;

import java.time.Instant;
import java.util.List;

public class Withdrawal extends Entry {
	public Withdrawal(String avatar, Instant date, List<Item> items) {
		super(avatar, date, items);
	}

	@Override
	protected String getType() {
		return "Entnahme";
	}
}
