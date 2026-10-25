package dev.schoenberg.evergore.protocolParser.acceptance.world;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ServiceLog {
	private static final String ADMIN_KEY_ANNOUNCEMENT = "Console admin key, valid until the next restart: ";

	private final List<String> lines = new CopyOnWriteArrayList<>();

	public void write(String line) {
		lines.add(line);
	}

	public List<String> lines() {
		return List.copyOf(lines);
	}

	public String adminKeyNamed() {
		return lines()
				.stream()
				.filter(line -> line.startsWith(ADMIN_KEY_ANNOUNCEMENT))
				.reduce((earlier, later) -> later)
				.map(line -> line.substring(ADMIN_KEY_ANNOUNCEMENT.length()))
				.orElseThrow(() -> new AssertionError("The service's log names no admin key"));
	}
}
