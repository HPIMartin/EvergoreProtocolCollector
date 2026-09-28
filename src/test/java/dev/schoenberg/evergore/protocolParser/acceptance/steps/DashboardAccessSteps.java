package dev.schoenberg.evergore.protocolParser.acceptance.steps;

import io.cucumber.java.en.When;

import dev.schoenberg.evergore.protocolParser.acceptance.browser.MemberBrowser;
import dev.schoenberg.evergore.protocolParser.acceptance.browser.TokenChoice;

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

	@When("a member opens the dashboard's start page {tokenChoice}")
	public void aMemberOpensTheDashboardsStartPage(TokenChoice choice) {
		browser.openTheStartPage(choice);
	}
}
