package dev.schoenberg.evergore.protocolParser.acceptance.world;

import dev.schoenberg.evergore.protocolParser.acceptance.browser.MemberBrowser;

public class ViewedBrowser {
	private MemberBrowser current;

	public ViewedBrowser(MemberBrowser firstBrowser) {
		current = firstBrowser;
	}

	public void lookAt(MemberBrowser browser) {
		current = browser;
	}

	public MemberBrowser current() {
		return current;
	}
}
