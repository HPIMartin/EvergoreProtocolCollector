package dev.schoenberg.evergore.protocolParser.acceptance.steps;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import dev.schoenberg.evergore.protocolParser.acceptance.browser.MemberBrowser;
import dev.schoenberg.evergore.protocolParser.acceptance.browser.TokenChoice;

import static dev.schoenberg.evergore.protocolParser.acceptance.browser.LedgerName.BANK;
import static dev.schoenberg.evergore.protocolParser.acceptance.browser.LedgerName.STORAGE;
import static dev.schoenberg.evergore.protocolParser.acceptance.steps.Pages.OVERVIEW;
import static dev.schoenberg.evergore.protocolParser.acceptance.steps.Pages.UNKNOWN;
import static org.assertj.core.api.Assertions.assertThat;

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

	@When("a member opens a bookmark of the overview {tokenChoice}")
	public void aMemberOpensABookmarkOfTheOverview(TokenChoice choice) {
		browser.openADeepLinkBookmark(OVERVIEW, choice);
	}

	@When("a member opens a bookmark of the bank ledger of {string} {tokenChoice}")
	public void aMemberOpensABookmarkOfTheBankLedgerOf(String avatar, TokenChoice choice) {
		browser.openADeepLinkBookmark(BANK.pathOf(avatar), choice);
	}

	@When("a member opens a bookmark of the storage ledger of {string} {tokenChoice}")
	public void aMemberOpensABookmarkOfTheStorageLedgerOf(String avatar, TokenChoice choice) {
		browser.openADeepLinkBookmark(STORAGE.pathOf(avatar), choice);
	}

	@When("a member opens a bookmark of a page the dashboard does not have {tokenChoice}")
	public void aMemberOpensABookmarkOfAPageTheDashboardDoesNotHave(TokenChoice choice) {
		browser.openADeepLinkBookmark(UNKNOWN, choice);
	}

	@Then("the browser shows no page of the dashboard")
	public void theBrowserShowsNoPageOfTheDashboard() {
		assertThat(browser.showsNoPageOfTheDashboard()).isTrue();
	}
}
