package dev.schoenberg.evergore.protocolParser.acceptance.world;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class ScenarioPasswords {
	private final Map<String, String> latestPerAvatar = new ConcurrentHashMap<>();

	public void record(String avatar, String password) {
		latestPerAvatar.put(avatar, password);
	}

	public Optional<String> issuedTo(String avatar) {
		return Optional.ofNullable(latestPerAvatar.get(avatar));
	}
}
