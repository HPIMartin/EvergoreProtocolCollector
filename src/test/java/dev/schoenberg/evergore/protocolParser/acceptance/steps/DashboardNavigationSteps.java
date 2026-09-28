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

import static dev.schoenberg.evergore.protocolParser.acceptance.browser.LedgerName.BANK;
import static dev.schoenberg.evergore.protocolParser.acceptance.browser.LedgerName.STORAGE;
import static dev.schoenberg.evergore.protocolParser.acceptance.steps.Pages.OVERVIEW;
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
