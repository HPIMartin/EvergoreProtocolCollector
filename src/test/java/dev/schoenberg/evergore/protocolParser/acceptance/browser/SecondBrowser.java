package dev.schoenberg.evergore.protocolParser.acceptance.browser;

import dev.schoenberg.evergore.protocolParser.acceptance.service.RunningService;
import dev.schoenberg.evergore.protocolParser.acceptance.world.ServiceLog;

public final class SecondBrowser {
	private final MemberBrowser browser;

	public SecondBrowser(RunningService service, BrowserChoice choice, ServiceLog log) {
		this.browser = new MemberBrowser(service, choice, log);
	}

	public MemberBrowser browser() {
		return browser;
	}
}
