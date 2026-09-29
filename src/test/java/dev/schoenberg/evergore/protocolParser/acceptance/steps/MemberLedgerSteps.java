package dev.schoenberg.evergore.protocolParser.acceptance.steps;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import dev.schoenberg.evergore.protocolParser.acceptance.browser.Ledger;
import dev.schoenberg.evergore.protocolParser.acceptance.browser.LedgerName;
import dev.schoenberg.evergore.protocolParser.acceptance.browser.MemberBrowser;

import static dev.schoenberg.evergore.protocolParser.acceptance.browser.LedgerName.BANK;
import static dev.schoenberg.evergore.protocolParser.acceptance.browser.LedgerName.STORAGE;
import static org.assertj.core.api.Assertions.assertThat;

public class MemberLedgerSteps {
	private static final String OVERVIEW = "/overview";
	private static final String MINUTE_COLUMN = "Zeitpunkt";

	private final MemberBrowser browser;

	public MemberLedgerSteps(MemberBrowser browser) {
		this.browser = browser;
	}

	@When("a member opens the {ledger} ledger of {string}")
	public void aMemberOpensTheLedgerOf(LedgerName ledger, String member) {
		browser.open(ledger.pathOf(member));
	}

	@When("a member opens the bank ledger and the storage ledger of {string}")
	public void aMemberOpensBothLedgersOf(String member) {
		browser.open(BANK.pathOf(member));
		browser.open(STORAGE.pathOf(member));
	}

	@When("a member opens the storage ledgers of {string} and {string}")
	public void aMemberOpensTheStorageLedgersOf(String first, String second) {
		browser.open(STORAGE.pathOf(first));
		browser.open(STORAGE.pathOf(second));
	}

	@When("a member opens the overview and the storage ledger of {string}")
	public void aMemberOpensTheOverviewAndTheStorageLedgerOf(String member) {
		browser.open(STORAGE.pathOf(member));
		browser.open(OVERVIEW);
	}

	@Then("the {ledger} ledger of {string} shows exactly:")
	public void theLedgerShowsExactly(LedgerName ledger, String member, DataTable expected) {
		Ledger shown = ledgerOf(ledger, member);

		List<String> headers = expected.row(0);
		List<List<String>> projected = shown.rows().stream().map(cells -> headers.stream().map(header -> cells.get(columnOf(shown, header))).toList()).toList();
		assertThat(sameMinutesInAnyOrder(projected, headers)).isEqualTo(sameMinutesInAnyOrder(expected.cells().subList(1, expected.height()), headers));
	}

	@Then("the {ledger} ledger of {string} says {string}")
	public void theLedgerSays(LedgerName ledger, String member, String message) {
		Ledger shown = ledgerOf(ledger, member);

		assertThat(shown.emptyMessage()).isEqualTo(message);
	}

	private Ledger ledgerOf(LedgerName ledger, String member) {
		String path = ledger.pathOf(member);
		if (!browser.shows(path)) {
			browser.open(path);
		}
		return browser.read("read-ledger.js", Ledger.class);
	}

	private static int columnOf(Ledger ledger, String header) {
		int column = ledger.headers().indexOf(header);
		assertThat(column).as("the ledger's column " + header + " among " + ledger.headers()).isNotNegative();
		return column;
	}

	private static List<List<String>> sameMinutesInAnyOrder(List<List<String>> rows, List<String> headers) {
		int minute = headers.indexOf(MINUTE_COLUMN);
		List<List<String>> ordered = new ArrayList<>();
		List<List<String>> sameMinute = new ArrayList<>();
		for (List<String> row : rows) {
			if (!sameMinute.isEmpty() && (minute < 0 || !sameMinute.getFirst().get(minute).equals(row.get(minute)))) {
				ordered.addAll(sorted(sameMinute));
				sameMinute.clear();
			}
			sameMinute.add(row);
		}
		ordered.addAll(sorted(sameMinute));
		return ordered;
	}

	private static List<List<String>> sorted(List<List<String>> rows) {
		return rows.stream().sorted(Comparator.comparing(List::toString)).toList();
	}
}
