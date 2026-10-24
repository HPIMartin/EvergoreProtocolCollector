package dev.schoenberg.evergore.protocolParser.application;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LoginGenerations {
	private final Map<String, Long> byNameKey = new ConcurrentHashMap<>();

	public long of(String nameKey) {
		return byNameKey.getOrDefault(nameKey, 0L);
	}

	public void bump(String nameKey) {
		byNameKey.merge(nameKey, 1L, Long::sum);
	}
}
