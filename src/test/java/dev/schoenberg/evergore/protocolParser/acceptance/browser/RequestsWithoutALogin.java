package dev.schoenberg.evergore.protocolParser.acceptance.browser;

public final class RequestsWithoutALogin {
	private static final String ASK_FOR_THE_HEALTH_CHECK = "const answered = arguments[arguments.length - 1];"
			+ " Promise.all(Array.from({ length: arguments[0] }, () => fetch('/health', { credentials: 'omit' }))).then(() => answered(true), () => answered(true));";

	private final MemberBrowser browser;

	RequestsWithoutALogin(MemberBrowser browser) {
		this.browser = browser;
	}

	public void send(int count) {
		browser.executeAsynchronously(ASK_FOR_THE_HEALTH_CHECK, count);
	}
}
