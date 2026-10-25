package dev.schoenberg.evergore.protocolParser.acceptance.operator;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public record Answer(int status, String body, Map<String, List<String>> headers) {
	public Optional<String> header(String name) {
		return headersNamed(name).stream().findFirst();
	}

	public List<String> headersNamed(String name) {
		return headers.getOrDefault(name.toLowerCase(Locale.ROOT), List.of());
	}
}
