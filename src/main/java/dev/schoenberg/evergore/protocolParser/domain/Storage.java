package dev.schoenberg.evergore.protocolParser.domain;

import java.time.*;
import java.util.*;

public class Storage extends Entry {
	public Storage(String avatar, Instant date, List<Item> items) {
		super(avatar, date, items);
	}

	@Override
	public String getType() {
		return "Einlagerung";
	}
}
