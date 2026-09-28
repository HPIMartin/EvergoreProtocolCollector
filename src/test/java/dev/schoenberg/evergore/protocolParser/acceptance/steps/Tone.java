package dev.schoenberg.evergore.protocolParser.acceptance.steps;

public enum Tone {
	CREDIT,
	DEBIT,
	NEUTRAL;

	public static Tone of(String phrase) {
		return switch (phrase) {
			case "in the credit colour" -> CREDIT;
			case "in the debit colour" -> DEBIT;
			case "uncoloured" -> NEUTRAL;
			default -> throw new IllegalArgumentException("No tone is named \"" + phrase + "\"");
		};
	}
}
