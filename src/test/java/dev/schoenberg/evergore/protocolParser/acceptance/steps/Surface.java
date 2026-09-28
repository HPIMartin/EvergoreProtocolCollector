package dev.schoenberg.evergore.protocolParser.acceptance.steps;

public enum Surface {
	ADMIN_PAGE,
	HEALTH_REPORT;

	public static Surface of(String text) {
		return switch (text) {
			case "the admin page" -> ADMIN_PAGE;
			case "the health report" -> HEALTH_REPORT;
			default -> throw new IllegalArgumentException("No surface is named \"" + text + "\"");
		};
	}
}
