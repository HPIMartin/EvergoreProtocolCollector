package dev.schoenberg.evergore.protocolParser.acceptance.steps;

import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import dev.schoenberg.evergore.protocolParser.acceptance.browser.LedgerName;
import dev.schoenberg.evergore.protocolParser.acceptance.browser.MemberBrowser;
import dev.schoenberg.evergore.protocolParser.acceptance.browser.MemberLedger;
import dev.schoenberg.evergore.protocolParser.acceptance.service.RunningService;
import dev.schoenberg.evergore.protocolParser.acceptance.world.GameProtocols;
import dev.schoenberg.evergore.protocolParser.acceptance.world.Guild;

import static dev.schoenberg.evergore.protocolParser.acceptance.browser.LedgerName.BANK;
import static dev.schoenberg.evergore.protocolParser.acceptance.browser.LedgerName.STORAGE;
import static dev.schoenberg.evergore.protocolParser.acceptance.browser.MemberLedger.MINUTE_COLUMN;
import static dev.schoenberg.evergore.protocolParser.acceptance.browser.MemberLedger.QUANTITY_COLUMN;
import static dev.schoenberg.evergore.protocolParser.acceptance.steps.Pages.OVERVIEW;
import static dev.schoenberg.evergore.protocolParser.acceptance.world.GameProtocol.BASE_MOVEMENT;
import static org.assertj.core.api.Assertions.assertThat;

public class MemberLedgerSteps {
	private static final String ZURUECK = "Zurück";
	private static final String WEITER = "Weiter";

	private final MemberBrowser browser;
	private final GameProtocols protocols;
	private final Guild guild;
	private final RunningService service;
	private int movementsInTheStorageLedger;

	public MemberLedgerSteps(MemberBrowser browser, GameProtocols protocols, Guild guild, RunningService service) {
		this.browser = browser;
		this.protocols = protocols;
		this.guild = guild;
		this.service = service;
	}

	@Given("{word} has {int} movements in the guild storage ledger")
	public void hasMovementsInTheGuildStorageLedger(String avatar, int count) {
		guild.join(avatar);
		movementsInTheStorageLedger = count;
		IntStream.rangeClosed(1, count).forEach(quantity -> protocols.storage().record(BASE_MOVEMENT.plusMinutes(quantity), avatar, "Einlagerung", quantity + " Kupfererz (100)"));
		service.storeWhatTheGameProtocolsShow();
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

	@When("a member opens page {int} of the storage ledger of {string}")
	public void aMemberOpensPageOfTheStorageLedgerOf(int page, String avatar) {
		browser.open(STORAGE.pathOf(avatar) + "?page=" + (page - 1));
	}

	@Given("a member has followed {string} in the storage ledger of {string}")
	public void aMemberHasFollowedInTheStorageLedgerOf(String link, String avatar) {
		browser.open(STORAGE.pathOf(avatar));
		browser.follow(link);
	}

	@When("the member later opens their bookmark of page {int} of the storage ledger of {string}")
	public void theMemberLaterOpensTheirBookmarkOfPageOfTheStorageLedgerOf(int page, String avatar) {
		browser.openExactly(browser.lastReachedUrl());

		URI opened = URI.create(browser.lastReachedUrl());
		assertThat(opened.getRawPath()).as("the ledger the bookmark opened").isEqualTo(STORAGE.pathOf(avatar));
		assertThat(queryParametersOf(opened)).as("the page the bookmark opened").contains("page=" + (page - 1));
	}

	@When("a member opens a bookmark of page {word} of the bank ledger of {string}")
	public void aMemberOpensABookmarkOfPageOfTheBankLedgerOf(String page, String avatar) {
		browser.open(BANK.pathOf(avatar) + "?page=" + queryPageOf(page));
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

	@Then("the ledger offers {string} but not {string}")
	public void theLedgerOffersButNot(String offered, String notOffered) {
		assertThat(isOffered(offered)).isTrue();
		assertThat(isOffered(notOffered)).isFalse();
	}

	@Then("the ledger says {string}")
	public void theLedgerSays(String message) {
		assertThat(ledger().emptyMessage()).isEqualTo(message);
	}

	@Then("the ledger shows her {int} newest movements")
	public void theLedgerShowsHerNewestMovements(int count) {
		assertThat(quantitiesShown()).isEqualTo(descendingRange(movementsInTheStorageLedger, movementsInTheStorageLedger - count + 1));
	}

	@Then("the ledger shows her {int} oldest movements")
	public void theLedgerShowsHerOldestMovements(int count) {
		assertThat(quantitiesShown()).isEqualTo(descendingRange(count, 1));
	}

	@Then("the ledger shows her movements {int} to {int}, counted from the newest")
	public void theLedgerShowsHerMovementsCountedFromTheNewest(int from, int to) {
		assertThat(quantitiesShown()).isEqualTo(descendingRange(movementsInTheStorageLedger - from + 1, movementsInTheStorageLedger - to + 1));
	}

	private List<Integer> quantitiesShown() {
		MemberLedger ledger = ledger();
		int column = ledger.columnOf(QUANTITY_COLUMN);
		return ledger.rows().stream().map(row -> Integer.parseInt(row.get(column))).toList();
	}

	private static List<Integer> descendingRange(int from, int to) {
		return IntStream.rangeClosed(to, from).boxed().sorted(Comparator.reverseOrder()).toList();
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

	private static List<String> queryParametersOf(URI address) {
		String query = address.getRawQuery();
		return query == null ? List.of() : List.of(query.split("&"));
	}

	private static String queryPageOf(String page) {
		try {
			return String.valueOf(Integer.parseInt(page) - 1);
		} catch (NumberFormatException notANumber) {
			return page;
		}
	}
}
