package dev.schoenberg.evergore.protocolParser.application;

import java.util.Map;
import java.util.OptionalLong;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;

public class LoginGenerations {
	private final Map<String, Long> byNameKey = new ConcurrentHashMap<>();
	private final Map<String, Object> lockByNameKey = new ConcurrentHashMap<>();

	public long of(String nameKey) {
		return byNameKey.getOrDefault(nameKey, 0L);
	}

	public void bump(String nameKey, Runnable change) {
		bumpIf(nameKey, () -> {
			change.run();
			return true;
		});
	}

	public boolean bumpIf(String nameKey, BooleanSupplier change) {
		synchronized (lockByNameKey.computeIfAbsent(nameKey, key -> new Object())) {
			boolean changed = change.getAsBoolean();
			if (changed) {
				byNameKey.put(nameKey, of(nameKey) + 1);
			}
			return changed;
		}
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
