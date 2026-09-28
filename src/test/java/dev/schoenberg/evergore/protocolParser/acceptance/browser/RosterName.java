package dev.schoenberg.evergore.protocolParser.acceptance.browser;

public enum RosterName {
	ACTIVE,
	DORMANT;

	public static RosterName of(String word) {
		return valueOf(word.toUpperCase());
	}
}
