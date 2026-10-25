package dev.schoenberg.evergore.protocolParser.application;

import java.util.Map;
import java.util.OptionalLong;
import java.util.concurrent.ConcurrentHashMap;

public class LoginGenerations {
	private final Map<String, Long> byNameKey = new ConcurrentHashMap<>();
	private final Map<String, Object> lockByNameKey = new ConcurrentHashMap<>();

	public long of(String nameKey) {
		return byNameKey.getOrDefault(nameKey, 0L);
	}

	public OptionalLong bumpWhileAt(String nameKey, long expected, Runnable change) {
		synchronized (lockByNameKey.computeIfAbsent(nameKey, key -> new Object())) {
			long held = of(nameKey);
			if (held != expected) {
				return OptionalLong.empty();
			}
			change.run();
			byNameKey.put(nameKey, held + 1);
			return OptionalLong.of(held + 1);
		}
	}
}
