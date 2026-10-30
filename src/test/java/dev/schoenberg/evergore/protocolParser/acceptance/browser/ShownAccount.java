package dev.schoenberg.evergore.protocolParser.acceptance.browser;

import java.util.List;

public record ShownAccount(String name, String rests, String lastLogin, String issued) {
	public List<String> cells() {
		return List.of(name, rests, lastLogin);
	}
}
