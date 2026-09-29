package dev.schoenberg.evergore.protocolParser.acceptance.browser;

import java.net.URLEncoder;

import static java.nio.charset.StandardCharsets.UTF_8;

public enum LedgerName {
	BANK("bank"),
	STORAGE("storage");

	private final String segment;

	LedgerName(String segment) {
		this.segment = segment;
	}

	public static LedgerName of(String word) {
		return valueOf(word.toUpperCase());
	}

	public String pathOf(String member) {
		return "/avatars/" + URLEncoder.encode(member, UTF_8).replace("+", "%20") + "/" + segment;
	}
}
