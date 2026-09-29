package dev.schoenberg.evergore.protocolParser.acceptance.world;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ServiceLog {
	private final List<String> lines = new CopyOnWriteArrayList<>();

	public void write(String line) {
		lines.add(line);
	}

	public List<String> lines() {
		return List.copyOf(lines);
	}
}
