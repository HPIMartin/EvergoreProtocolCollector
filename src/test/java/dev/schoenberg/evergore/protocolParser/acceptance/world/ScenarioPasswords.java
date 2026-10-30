package dev.schoenberg.evergore.protocolParser.acceptance.world;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class ScenarioPasswords {
	private final Map<String, List<String>> inOrderPerAvatar = new ConcurrentHashMap<>();

	public void record(String avatar, String password) {
		inOrderPerAvatar.computeIfAbsent(avatar, ignored -> new CopyOnWriteArrayList<>()).add(password);
	}

	public Optional<String> issuedTo(String avatar) {
		List<String> issued = inOrderPerAvatar.getOrDefault(avatar, List.of());
		return issued.isEmpty() ? Optional.empty() : Optional.of(issued.getLast());
	}

	public Optional<String> firstIssuedTo(String avatar) {
		return inOrderPerAvatar.getOrDefault(avatar, List.of()).stream().findFirst();
	}
}
