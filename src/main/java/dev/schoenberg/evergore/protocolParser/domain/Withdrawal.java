package dev.schoenberg.evergore.protocolParser.domain;

import java.time.*;
import java.util.*;

public class Withdrawal extends Entry {
	public static final String ENTNAHME = "Entnahme";

	public Withdrawal(String avatar, Instant date, List<Item> items) {
		super(avatar, date, items);
	}

	@Override
	public String getType() {
		return ENTNAHME;
	}
}
