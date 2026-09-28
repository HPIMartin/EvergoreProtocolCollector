package dev.schoenberg.evergore.protocolParser.acceptance.browser;

public enum TokenChoice {
	WITHOUT_TOKEN,
	WRONG_TOKEN;

	public static TokenChoice of(String phrase) {
		return switch (phrase) {
			case "without the token" -> WITHOUT_TOKEN;
			case "with a wrong token" -> WRONG_TOKEN;
			default -> throw new IllegalArgumentException("Unknown token choice: " + phrase);
		};
	}
}
