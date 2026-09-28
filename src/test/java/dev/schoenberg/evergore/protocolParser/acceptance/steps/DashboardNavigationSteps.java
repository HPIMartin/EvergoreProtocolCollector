package dev.schoenberg.evergore.protocolParser.acceptance.steps;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import dev.schoenberg.evergore.protocolParser.acceptance.browser.MemberBrowser;
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

	@When("a member opens a link to a page the dashboard does not have")
	public void aMemberOpensALinkToAPageTheDashboardDoesNotHave() {
		browser.open(UNKNOWN);
	}

	@Then("the page says there is no view for that link")
	public void thePageSaysThereIsNoViewForThatLink() {
		assertThat(overview().messages()).anyMatch(message -> NO_VIEW_FOR_THAT_LINK.matcher(message).matches());
	}

	@Then("in {word}'s row the {string} leads to the bank ledger of {string}")
	public void inRowTheLeadsToTheBankLedgerOf(String owner, String header, String avatar) {
		assertThat(pathLedTo(owner, header)).isEqualTo(BANK.pathOf(avatar));
	}

	@Then("in {word}'s row the {string} leads to the storage ledger of {string}")
	public void inRowTheLeadsToTheStorageLedgerOf(String owner, String header, String avatar) {
		assertThat(pathLedTo(owner, header)).isEqualTo(STORAGE.pathOf(avatar));
	}

	@Then("in {word}'s row the {string} shows {string} and leads nowhere")
	public void inRowTheShowsAndLeadsNowhere(String owner, String header, String expected) {
		Overview.Placed placed = rowOf(owner);
		int column = columnOf(placed.headers(), header);

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
		assertThat(navigation().allHrefs()).noneMatch(href -> pathOnly(href).equals("/admin"));
	}

	private Navigation navigation() {
		return browser.navigation();
	}

	private Overview overview() {
		return browser.read("read-overview.js", Overview.class);
	}

	private String pathLedTo(String owner, String header) {
		Overview.Placed placed = rowOf(owner);
		int column = columnOf(placed.headers(), header);
		String href = placed.row().hrefs().get(column);
		assertThat(href).as("a link in " + owner + "'s " + header + " cell").isNotNull();
		return pathOnly(href);
	}

	private Overview.Placed rowOf(String owner) {
		return overview()
				.placedMemberRows()
				.stream()
				.filter(placed -> placed.row().cells().getFirst().equals(owner))
				.findFirst()
				.orElseThrow(() -> new AssertionError("The overview lists no row for " + owner));
	}

	private static int columnOf(List<String> headers, String header) {
		int column = headers.indexOf(header);
		assertThat(column).as("the overview's column " + header + " among " + headers).isNotNegative();
		return column;
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
