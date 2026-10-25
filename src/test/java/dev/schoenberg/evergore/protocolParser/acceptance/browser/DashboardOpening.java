package dev.schoenberg.evergore.protocolParser.acceptance.browser;

public class DashboardOpening {
	private final MemberBrowser browser;

	DashboardOpening(MemberBrowser browser) {
		this.browser = browser;
	}

	public void open(String path) {
		if (browser.hasALogin()) {
			browser.openHref(path);
		} else {
			browser.loginForm().logInWithTheAdminKeyToOpen(path);
		}
	}
}
