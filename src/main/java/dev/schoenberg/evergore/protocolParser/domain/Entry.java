package dev.schoenberg.evergore.protocolParser.domain;

import static dev.schoenberg.evergore.protocolParser.businessLogic.Constants.*;

import java.time.*;
import java.time.format.*;
import java.util.*;

import dev.schoenberg.evergore.protocolParser.businessLogic.base.*;

public record Entry(String avatar, Instant date, List<Item> items, TransferType type) {
	public String print(String delimiter) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(APP_ZONE);
		String prefix = type + delimiter + formatter.format(date) + delimiter + avatar + delimiter;
		List<String> result = new ArrayList<>();
		for (Item item : items) {
			result.add(prefix + item.toString(delimiter));
		}
		return result.stream().reduce((x, y) -> x + "\n" + y).orElse("");
	}
}
