package dev.schoenberg.evergore.protocolParser.acceptance.steps;

import io.cucumber.java.en.When;

import dev.schoenberg.evergore.protocolParser.acceptance.browser.MemberBrowser;

import static dev.schoenberg.evergore.protocolParser.acceptance.steps.Pages.OVERVIEW;

public class DashboardNavigationSteps {
	private final MemberBrowser browser;

	public DashboardNavigationSteps(MemberBrowser browser) {
		this.browser = browser;
	}

	@When("a member opens a bookmark of the overview")
	public void aMemberOpensABookmarkOfTheOverview() {
		browser.open(OVERVIEW);
	}
}
