package dev.schoenberg.evergore.protocolParser.acceptance.steps;

import io.cucumber.java.en.When;

import dev.schoenberg.evergore.protocolParser.acceptance.browser.MemberBrowser;

public class DashboardAccessSteps {
	private static final String START_PAGE = "/";

	private final MemberBrowser browser;

	public DashboardAccessSteps(MemberBrowser browser) {
		this.browser = browser;
	}

	@When("a member opens the guild's link")
	public void aMemberOpensTheGuildsLink() {
		browser.open(START_PAGE);
	}
}
