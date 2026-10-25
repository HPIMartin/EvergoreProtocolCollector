package dev.schoenberg.evergore.protocolParser.acceptance.browser;

import dev.schoenberg.evergore.protocolParser.acceptance.service.RunningService;

public final class SecondBrowser {
	private final MemberBrowser browser;

	public SecondBrowser(RunningService service, BrowserChoice choice) {
		this.browser = new MemberBrowser(service, choice);
	}

	public MemberBrowser browser() {
		return browser;
	}
}
