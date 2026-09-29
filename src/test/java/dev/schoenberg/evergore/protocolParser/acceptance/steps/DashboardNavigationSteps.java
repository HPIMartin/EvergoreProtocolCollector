package dev.schoenberg.evergore.protocolParser.acceptance.steps;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import dev.schoenberg.evergore.protocolParser.acceptance.browser.MemberBrowser;
import dev.schoenberg.evergore.protocolParser.acceptance.browser.MemberLedger;
import dev.schoenberg.evergore.protocolParser.acceptance.browser.Navigation;
import dev.schoenberg.evergore.protocolParser.acceptance.browser.Navigation.FrameLink;
import dev.schoenberg.evergore.protocolParser.acceptance.browser.Overview;

import static dev.schoenberg.evergore.protocolParser.acceptance.browser.LedgerName.BANK;
import static dev.schoenberg.evergore.protocolParser.acceptance.browser.LedgerName.STORAGE;
import static dev.schoenberg.evergore.protocolParser.acceptance.steps.Pages.NO_VIEW_FOR_THAT_LINK;
import static dev.schoenberg.evergore.protocolParser.acceptance.steps.Pages.OVERVIEW;
import static dev.schoenberg.evergore.protocolParser.acceptance.steps.Pages.UNKNOWN;
import static org.assertj.core.api.Assertions.assertThat;

public class DashboardNavigationSteps {
	private static final String YES = "yes";
	private static final String NO = "no";
	private static final Pattern BANK_LEDGER_OF = Pattern.compile("the bank ledger of \"([^\"]+)\"");
	private static final Pattern STORAGE_LEDGER_OF = Pattern.compile("the storage ledger of \"([^\"]+)\"");

	private final MemberBrowser browser;

	public DashboardNavigationSteps(MemberBrowser browser) {
		this.browser = browser;
	}

	@When("a member opens a bookmark of the overview")
	public void aMemberOpensABookmarkOfTheOverview() {
		browser.open(OVERVIEW);
	}

	@When("the member follows the name of {string} in the overview")
	public void theMemberFollowsTheNameOfInTheOverview(String member) {
		browser.follow(member);
	}

	@Then("the ledger shows a movement of {word} gold")
	public void theLedgerShowsAMovementOfGold(String gold) {
		MemberLedger ledger = browser.ledger();
		int column = ledger.columnOf("Betrag");

		assertThat(ledger.rows()).extracting(row -> row.get(column)).contains(gold);
	}

	@When("a member opens a link to a page the dashboard does not have")
	public void aMemberOpensALinkToAPageTheDashboardDoesNotHave() {
		browser.open(UNKNOWN);
	}

	@Then("the page says there is no view for that link")
	public void thePageSaysThereIsNoViewForThatLink() {
		Overview overview = browser.overview();
		assertThat(overview.messages()).anyMatch(message -> NO_VIEW_FOR_THAT_LINK.matcher(message).matches());
	}

	@Then("in {word}'s row the {string} leads to the bank ledger of {string}")
	public void inRowTheLeadsToTheBankLedgerOf(String owner, String header, String avatar) {
		String path = pathLedTo(owner, header);
		assertThat(path).isEqualTo(BANK.pathOf(avatar));
	}

	@Then("in {word}'s row the {string} leads to the storage ledger of {string}")
	public void inRowTheLeadsToTheStorageLedgerOf(String owner, String header, String avatar) {
		String path = pathLedTo(owner, header);
		assertThat(path).isEqualTo(STORAGE.pathOf(avatar));
	}

	@Then("in {word}'s row the {string} shows {string} and leads nowhere")
	public void inRowTheShowsAndLeadsNowhere(String owner, String header, String expected) {
		Overview.Placed placed = browser.overview().rowOf(owner);
		int column = placed.columnOf(header);

		assertThat(placed.row().cells().get(column)).isEqualTo(expected);
		assertThat(placed.row().hrefs().get(column)).isNull();
	}

	@Then("the page's frame offers exactly the links:")
	public void thePagesFrameOffersExactlyTheLinks(DataTable expected) {
		List<List<String>> shown = navigation().frame().stream().map(DashboardNavigationSteps::rowOf).toList();
		List<List<String>> wanted = expected.cells().subList(1, expected.height()).stream().map(row -> List.of(row.get(0), pathOf(row.get(1)), row.get(2))).toList();

		assertThat(shown).isEqualTo(wanted);
	}

	@Then("no link on the page leads to the admin page")
	public void noLinkOnThePageLeadsToTheAdminPage() {
		Navigation navigation = navigation();
		assertThat(navigation.allHrefs()).noneMatch(href -> pathOnly(href).equals("/admin"));
	}

	private Navigation navigation() {
		return browser.navigation();
	}

	private String pathLedTo(String owner, String header) {
		Overview.Placed placed = browser.overview().rowOf(owner);
		int column = placed.columnOf(header);
		String href = placed.row().hrefs().get(column);
		assertThat(href).as("a link in " + owner + "'s " + header + " cell").isNotNull();
		return pathOnly(href);
	}

	private static List<String> rowOf(FrameLink link) {
		return List.of(link.label(), pathOnly(link.href()), link.current() ? YES : NO);
	}

	private static String pathOnly(String href) {
		int query = href.indexOf('?');
		return query < 0 ? href : href.substring(0, query);
	}

	private static String pathOf(String leadsTo) {
		Matcher bank = BANK_LEDGER_OF.matcher(leadsTo);
		if (bank.matches()) {
			return BANK.pathOf(bank.group(1));
		}
		Matcher storage = STORAGE_LEDGER_OF.matcher(leadsTo);
		if (storage.matches()) {
			return STORAGE.pathOf(storage.group(1));
		}
		if (leadsTo.equals("the overview")) {
			return OVERVIEW;
		}
		throw new IllegalArgumentException("Unknown link target: " + leadsTo);
	}
}
