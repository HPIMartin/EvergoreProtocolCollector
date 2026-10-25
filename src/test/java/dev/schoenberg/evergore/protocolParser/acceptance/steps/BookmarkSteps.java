package dev.schoenberg.evergore.protocolParser.acceptance.steps;

import java.net.URI;
import java.net.URLEncoder;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.cucumber.java.ParameterType;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import dev.schoenberg.evergore.protocolParser.acceptance.browser.MemberBrowser;

import static dev.schoenberg.evergore.protocolParser.acceptance.browser.LedgerName.BANK;
import static dev.schoenberg.evergore.protocolParser.acceptance.browser.LedgerName.STORAGE;
import static dev.schoenberg.evergore.protocolParser.acceptance.steps.Pages.EXPLANATION;
import static dev.schoenberg.evergore.protocolParser.acceptance.steps.Pages.OVERVIEW;
import static dev.schoenberg.evergore.protocolParser.acceptance.steps.Pages.UNKNOWN;
import static dev.schoenberg.evergore.protocolParser.acceptance.world.OperatorSettings.GUILD_TOKEN;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

public class BookmarkSteps {
	private static final String START_PAGE = "/";
	private static final String ADMIN = "/admin";
	private static final String LOGIN_PAGE = "/login";
	private static final String LOGIN = "Anmeldung";
	private static final Pattern BANK_LEDGER_OF = Pattern.compile("the bank ledger of \"([^\"]+)\"");
	private static final Pattern STORAGE_LEDGER_OF = Pattern.compile("the storage ledger of \"([^\"]+)\"");
	private static final Pattern SECOND_PAGE_OF_THE_BANK_LEDGER_OF = Pattern.compile("page 2 of the bank ledger of \"([^\"]+)\"");
	private static final String SECOND_PAGE = "?page=1";

	private final MemberBrowser browser;

	public BookmarkSteps(MemberBrowser browser) {
		this.browser = browser;
	}

	@ParameterType("the dashboard's start page|the overview|the bank ledger of \"[^\"]+\"|the storage ledger of \"[^\"]+\"|page 2 of the bank ledger of \"[^\"]+\"|the explanation page"
			+ "|the admin page|a page the dashboard does not have|the guild's old link, from before members logged in")
	public String bookmark(String phrase) {
		Matcher bank = BANK_LEDGER_OF.matcher(phrase);
		Matcher storage = STORAGE_LEDGER_OF.matcher(phrase);
		Matcher secondPage = SECOND_PAGE_OF_THE_BANK_LEDGER_OF.matcher(phrase);
		if (bank.matches()) {
			return BANK.pathOf(bank.group(1));
		}
		if (storage.matches()) {
			return STORAGE.pathOf(storage.group(1));
		}
		if (secondPage.matches()) {
			return BANK.pathOf(secondPage.group(1)) + SECOND_PAGE;
		}
		return switch (phrase) {
			case "the dashboard's start page" -> START_PAGE;
			case "the overview" -> OVERVIEW;
			case "the explanation page" -> EXPLANATION;
			case "the admin page" -> ADMIN;
			case "a page the dashboard does not have" -> UNKNOWN;
			default -> START_PAGE + "?token=" + GUILD_TOKEN;
		};
	}

	@ParameterType("by its full address|disguised as this service's")
	public String awayLink(String how) {
		return "by its full address".equals(how) ? "https://evil.example/overview" : "//evil.example/overview";
	}

	@When("a member who is not logged in opens a bookmark of {bookmark}")
	public void aMemberWhoIsNotLoggedInOpensABookmarkOf(String path) {
		browser.openHref(path);
	}

	@When("the member opens a bookmark of the overview instead of choosing a new password")
	public void theMemberOpensABookmarkOfTheOverviewInsteadOfChoosingANewPassword() {
		browser.openHref(OVERVIEW);
	}

	@Given("a member who is not logged in has opened a bookmark of {bookmark}")
	public void aMemberWhoIsNotLoggedInHasOpenedABookmarkOf(String path) {
		browser.openHref(path);
	}

	@Given("a member has followed a link to the login page that would lead on to another site {awayLink}")
	public void aMemberHasFollowedALinkToTheLoginPageThatWouldLeadAway(String target) {
		browser.openHref(LOGIN_PAGE + "?next=" + URLEncoder.encode(target, UTF_8));
	}

	@When("the member follows a link to the overview from a page of another site")
	public void theMemberFollowsALinkToTheOverviewFromAPageOfAnotherSite() {
		browser.followALinkFromAnotherSite(OVERVIEW);
	}

	@Then("a bookmark of the overview opened afterwards shows the login page")
	public void aBookmarkOfTheOverviewOpenedAfterwardsShowsTheLoginPage() {
		browser.openHref(OVERVIEW);

		assertThat(browser.ledger().heading()).isEqualTo(LOGIN);
	}

	@When("the member opens {bookmark}")
	public void theMemberOpens(String path) {
		browser.opening().open(path);
	}

	@Given("the member has opened {bookmark}")
	public void theMemberHasOpened(String path) {
		browser.opening().open(path);
	}

	@Given("the member has followed the name of {string} in the overview to the login page")
	public void theMemberHasFollowedTheNameOfInTheOverviewToTheLoginPage(String member) {
		browser.follow(member);

		assertThat(browser.ledger().heading()).isEqualTo(LOGIN);
	}

	@Then("every link on the page opens its page and none of them shows the login page")
	public void everyLinkOnThePageOpensItsPageAndNoneOfThemShowsTheLoginPage() {
		List<String> hrefs = browser.navigation().allHrefs();
		assertThat(hrefs).isNotEmpty();

		for (String href : hrefs) {
			browser.openHref(href);

			assertThat(URI.create(browser.lastReachedUrl()).getRawPath()).as("the page the link " + href + " opened").isEqualTo(URI.create(href).getRawPath());
			assertThat(browser.ledger().heading()).as("the heading of the page the link " + href + " opened").isNotEqualTo(LOGIN);
		}
	}
}
