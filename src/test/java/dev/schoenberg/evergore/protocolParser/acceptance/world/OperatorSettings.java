package dev.schoenberg.evergore.protocolParser.acceptance.world;

import java.util.LinkedHashMap;
import java.util.Map;

public class OperatorSettings {
	public static final String GUILD_TOKEN = "test-token";
	public static final String GUILD_TOKEN_VARIABLE = "EVERGORE_SECURITY_API_TOKEN";
	private static final String BLANK = " ";

	private final Map<String, Object> environment = new LinkedHashMap<>(
			Map.of("EVERGORE_CREDENTIALS_USERNAME", "acceptance-username", "EVERGORE_CREDENTIALS_PASSWORD", "acceptance-password"));
	private final Map<String, Object> properties = new LinkedHashMap<>();

	public synchronized void unset(String variable) {
		environment.remove(variable);
	}

	public synchronized void blank(String variable) {
		environment.put(variable, BLANK);
	}

	public synchronized void setVariable(String variable, String value) {
		environment.put(variable, value);
	}

	public synchronized void set(String property, String value) {
		properties.put(property, value);
	}

	public synchronized Map<String, Object> environment() {
		return Map.copyOf(environment);
	}

	public synchronized Map<String, Object> properties() {
		return Map.copyOf(properties);
	}
}
