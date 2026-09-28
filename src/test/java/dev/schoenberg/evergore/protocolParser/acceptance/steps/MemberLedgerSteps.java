package dev.schoenberg.evergore.protocolParser.acceptance.steps;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import dev.schoenberg.evergore.protocolParser.acceptance.browser.LedgerName;
import dev.schoenberg.evergore.protocolParser.acceptance.browser.MemberBrowser;
import dev.schoenberg.evergore.protocolParser.acceptance.browser.MemberLedger;

import static dev.schoenberg.evergore.protocolParser.acceptance.browser.LedgerName.BANK;
import static dev.schoenberg.evergore.protocolParser.acceptance.browser.LedgerName.STORAGE;
import static dev.schoenberg.evergore.protocolParser.acceptance.browser.MemberLedger.MINUTE_COLUMN;
import static org.assertj.core.api.Assertions.assertThat;

public class MemberLedgerSteps {
	private static final String OVERVIEW = "/overview";
	private static final String ZURUECK = "Zurück";
	private static final String WEITER = "Weiter";

	private final MemberBrowser browser;

	public MemberLedgerSteps(MemberBrowser browser) {
		this.browser = browser;
	}

	@When("a member opens the bank ledger of {string}")
	public void aMemberOpensTheBankLedgerOf(String avatar) {
		browser.open(BANK.pathOf(avatar));
	}

	@When("a member opens the storage ledger of {string}")
	public void aMemberOpensTheStorageLedgerOf(String avatar) {
		browser.open(STORAGE.pathOf(avatar));
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

	@Then("the page is headed {string}")
	public void thePageIsHeaded(String heading) {
		assertThat(ledger().heading()).isEqualTo(heading);
	}

	@Then("the bank ledger of {string} shows exactly:")
	public void theBankLedgerOfShowsExactly(String avatar, DataTable expected) {
		assertLedgerShowsExactly(avatar, BANK, expected);
	}

	@Then("the storage ledger of {string} shows exactly:")
	public void theStorageLedgerOfShowsExactly(String avatar, DataTable expected) {
		assertLedgerShowsExactly(avatar, STORAGE, expected);
	}

	@Then("the {ledger} ledger of {string} says {string}")
	public void theNamedLedgerSays(LedgerName namedLedger, String member, String message) {
		browser.open(namedLedger.pathOf(member));
		assertThat(ledger().emptyMessage()).isEqualTo(message);
	}

	@Then("the ledger's caption reads {string}")
	public void theLedgersCaptionReads(String caption) {
		assertThat(ledger().caption()).isEqualTo(caption);
	}

	@Then("the ledger offers neither {string} nor {string}")
	public void theLedgerOffersNeitherNor(String first, String second) {
		assertThat(isOffered(first)).isFalse();
		assertThat(isOffered(second)).isFalse();
	}

	private void assertLedgerShowsExactly(String avatar, LedgerName ledgerName, DataTable expected) {
		browser.open(ledgerName.pathOf(avatar));
		MemberLedger ledger = ledger();
		assertThat(ledger.heading()).contains(avatar);
		List<String> headers = expected.row(0);
		assertThat(ledger.headers()).as("the ledger's columns in the order the scenario names them").containsSubsequence(headers);
		List<List<String>> projected = ledger.rows().stream().map(cells -> headers.stream().map(header -> cells.get(ledger.columnOf(header))).toList()).toList();
		assertThat(sameMinutesInAnyOrder(projected, headers)).isEqualTo(sameMinutesInAnyOrder(expected.cells().subList(1, expected.height()), headers));
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

	private boolean isOffered(String label) {
		MemberLedger ledger = ledger();
		return switch (label) {
			case ZURUECK -> ledger.hasPrevious();
			case WEITER -> ledger.hasNext();
			default -> throw new IllegalArgumentException("Unknown pagination label: " + label);
		};
	}

	private MemberLedger ledger() {
		return browser.read("read-ledger.js", MemberLedger.class);
	}
}
