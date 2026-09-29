package dev.schoenberg.evergore.protocolParser.acceptance.world;

import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

public class OperatorSettings {
	public static final String GUILD_TOKEN = "test-token";
	private static final String BLANK = " ";

	private final Map<String, Object> environment = new LinkedHashMap<>(
			Map.of("EVERGORE_SECURITY_API_TOKEN", GUILD_TOKEN, "EVERGORE_CREDENTIALS_USERNAME", "acceptance-username", "EVERGORE_CREDENTIALS_PASSWORD", "acceptance-password"));
	private final Map<String, Object> properties = new LinkedHashMap<>();
	private volatile ZoneId zone = ZoneId.systemDefault();

	public synchronized void unset(String variable) {
		environment.remove(variable);
	}

	public synchronized void blank(String variable) {
		environment.put(variable, BLANK);
	}

	public synchronized void set(String property, String value) {
		properties.put(property, value);
	}

	public void runIn(ZoneId other) {
		zone = other;
	}

	public synchronized Map<String, Object> environment() {
		return Map.copyOf(environment);
	}

	public synchronized Map<String, Object> properties() {
		return Map.copyOf(properties);
	}

	public ZoneId zone() {
		return zone;
	}
}
