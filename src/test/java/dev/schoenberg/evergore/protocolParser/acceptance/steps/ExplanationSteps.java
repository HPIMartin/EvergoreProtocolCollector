package dev.schoenberg.evergore.protocolParser.acceptance.steps;

import io.cucumber.java.en.When;

import dev.schoenberg.evergore.protocolParser.acceptance.browser.MemberBrowser;
import dev.schoenberg.evergore.protocolParser.acceptance.browser.TokenChoice;

import static dev.schoenberg.evergore.protocolParser.acceptance.steps.Pages.EXPLANATION;

public class ExplanationSteps {
	private static final String START_PAGE = "/";
	private static final String LINK_FROM_THE_OVERVIEW = "Wie entstehen diese Zahlen?";

	private final MemberBrowser browser;

	public ExplanationSteps(MemberBrowser browser) {
		this.browser = browser;
	}

	@When("a member opens the explanation page")
	public void aMemberOpensTheExplanationPage() {
		browser.open(EXPLANATION);
	}

	@When("a member opens a bookmark of the explanation page {tokenChoice}")
	public void aMemberOpensABookmarkOfTheExplanationPage(TokenChoice choice) {
		browser.openADeepLinkBookmark(EXPLANATION, choice);
	}

	@When("a member follows the guild's link to the explanation page")
	public void aMemberFollowsTheGuildsLinkToTheExplanationPage() {
		browser.open(START_PAGE);
		browser.follow(LINK_FROM_THE_OVERVIEW);
	}

	@When("the member follows {string}")
	public void theMemberFollows(String linkText) {
		browser.follow(linkText);
	}
}
