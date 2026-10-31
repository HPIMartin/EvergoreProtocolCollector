package dev.schoenberg.evergore.protocolParser.acceptance.browser;

import java.util.Arrays;
import java.util.List;

public record ShownAccount(String name, String role, List<String> roleChoices, String rests, String lastLogin, String issued) {
	public List<String> cells() {
		return Arrays.asList(name, role, rests, lastLogin);
	}
}
