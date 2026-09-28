package dev.schoenberg.evergore.protocolParser.acceptance.steps;

import java.net.URI;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import dev.schoenberg.evergore.protocolParser.acceptance.browser.MemberBrowser;
import dev.schoenberg.evergore.protocolParser.acceptance.browser.Overview;
import dev.schoenberg.evergore.protocolParser.acceptance.browser.TokenChoice;
import dev.schoenberg.evergore.protocolParser.acceptance.service.RunningService;
import dev.schoenberg.evergore.protocolParser.acceptance.world.GameProtocols;
import dev.schoenberg.evergore.protocolParser.acceptance.world.Guild;

import static dev.schoenberg.evergore.protocolParser.acceptance.browser.LedgerName.BANK;
import static dev.schoenberg.evergore.protocolParser.acceptance.browser.LedgerName.STORAGE;
import static dev.schoenberg.evergore.protocolParser.acceptance.steps.Pages.NO_VIEW_FOR_THAT_LINK;
import static dev.schoenberg.evergore.protocolParser.acceptance.steps.Pages.OVERVIEW;
import static dev.schoenberg.evergore.protocolParser.acceptance.steps.Pages.UNKNOWN;
import static dev.schoenberg.evergore.protocolParser.acceptance.world.GameProtocol.BASE_MOVEMENT;
import static org.assertj.core.api.Assertions.assertThat;

public class DashboardAccessSteps {
	private static final String START_PAGE = "/";
	private static final Pattern LOAD_FAILURE = Pattern.compile("Fehler: .*");

	private final MemberBrowser browser;
	private final GameProtocols protocols;
	private final Guild guild;
	private final RunningService service;

	public DashboardAccessSteps(MemberBrowser browser, GameProtocols protocols, Guild guild, RunningService service) {
		this.browser = browser;
		this.protocols = protocols;
		this.guild = guild;
		this.service = service;
	}

	@Given("{word} has {int} movements in the guild bank ledger")
	public void hasMovementsInTheGuildBankLedger(String avatar, int count) {
		guild.join(avatar);
		IntStream.rangeClosed(1, count).forEach(minute -> protocols.bank().record(BASE_MOVEMENT.plusMinutes(minute), avatar, "Einzahlung", minute + " Gold"));
		service.storeWhatTheGameProtocolsShow();
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

	@When("a member follows the guild's link to the overview")
	public void aMemberFollowsTheGuildsLinkToTheOverview() {
		browser.open(START_PAGE);
	}

	@When("a member follows the guild's link to the bank ledger of {string}")
	public void aMemberFollowsTheGuildsLinkToTheBankLedgerOf(String avatar) {
		browser.open(START_PAGE);
		browser.follow(avatar);
	}

	@Then("every link on the page opens its page and none of them says {string}")
	public void everyLinkOnThePageOpensItsPageAndNoneOfThemSays(String forbiddenMessage) {
		List<String> hrefs = browser.navigation().allHrefs();
		assertThat(hrefs).isNotEmpty();

		for (String href : hrefs) {
			browser.openHref(href);
			String reachedPath = URI.create(browser.lastReachedUrl()).getRawPath();
			List<String> messages = browser.read("read-overview.js", Overview.class).messages();

			assertThat(reachedPath).as("the page the link " + href + " opened").isEqualTo(URI.create(href).getRawPath());
			assertThat(messages).as("the token complaint on " + href).doesNotContain(forbiddenMessage);
			assertThat(messages).as("an unknown-view message on " + href).noneMatch(message -> NO_VIEW_FOR_THAT_LINK.matcher(message).matches());
			assertThat(messages).as("a load-failure message on " + href).noneMatch(message -> LOAD_FAILURE.matcher(message).matches());
		}
	}
}
