package dev.schoenberg.evergore.protocolParser.acceptance.steps;

import java.util.function.Supplier;
import java.util.stream.IntStream;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import dev.schoenberg.evergore.protocolParser.acceptance.browser.MemberBrowser;
import dev.schoenberg.evergore.protocolParser.acceptance.browser.MemberLedger;
import dev.schoenberg.evergore.protocolParser.acceptance.browser.Overview;
import dev.schoenberg.evergore.protocolParser.acceptance.browser.RosterName;
import dev.schoenberg.evergore.protocolParser.acceptance.browser.Sort;
import dev.schoenberg.evergore.protocolParser.acceptance.service.RunningService;
import dev.schoenberg.evergore.protocolParser.acceptance.world.GameProtocols;
import dev.schoenberg.evergore.protocolParser.acceptance.world.Guild;

import static dev.schoenberg.evergore.protocolParser.acceptance.browser.LedgerName.STORAGE;
import static dev.schoenberg.evergore.protocolParser.acceptance.browser.MemberLedger.QUANTITY_COLUMN;
import static dev.schoenberg.evergore.protocolParser.acceptance.steps.Pages.OVERVIEW;
import static dev.schoenberg.evergore.protocolParser.acceptance.world.GameProtocol.BASE_MOVEMENT;
import static org.assertj.core.api.Assertions.assertThat;

public class DashboardSortingSteps {
	private static final int MAX_CLICKS_TO_REACH_A_DIRECTION = 2;
	private static final String ASCENDING = "ascending";

	private final MemberBrowser browser;
	private final GameProtocols protocols;
	private final Guild guild;
	private final RunningService service;

	public DashboardSortingSteps(MemberBrowser browser, GameProtocols protocols, Guild guild, RunningService service) {
		this.browser = browser;
		this.protocols = protocols;
		this.guild = guild;
		this.service = service;
	}

	@Given("{word} has {int} movements of {string} in the guild storage ledger, the newest of quantity {int} and each older one of one more")
	public void hasMovementsOfItemInTheGuildStorageLedger(String avatar, int count, String item, int newestQuantity) {
		guild.join(avatar);
		IntStream.rangeClosed(1, count).forEach(rank -> {
			int quantity = newestQuantity + count - rank;
			protocols.storage().record(BASE_MOVEMENT.plusMinutes(rank), avatar, "Einlagerung", quantity + " " + item + " (100)");
		});
		service.storeWhatTheGameProtocolsShow();
	}

	@Given("a member has opened the storage ledger of {string}")
	public void aMemberHasOpenedTheStorageLedgerOf(String avatar) {
		browser.open(STORAGE.pathOf(avatar));
	}

	@Given("a member has sorted the storage ledger of {string} by {string} in {direction} order")
	public void aMemberHasSortedTheStorageLedgerOfBy(String avatar, String header, String direction) {
		browser.open(STORAGE.pathOf(avatar));
		sortLedgerBy(header, direction);
	}

	@Given("the member has followed {string}")
	public void theMemberHasFollowed(String link) {
		browser.follow(link);
	}

	@When("the member sorts the ledger by {string} in {direction} order")
	public void theMemberSortsTheLedgerBy(String header, String direction) {
		sortLedgerBy(header, direction);
	}

	@Then("the ledger's first row shows a quantity of {int}")
	public void theLedgersFirstRowShowsAQuantityOf(int quantity) {
		MemberLedger ledger = ledger();
		int column = ledger.columnOf(QUANTITY_COLUMN);

		assertThat(ledger.rows().getFirst().get(column)).isEqualTo(String.valueOf(quantity));
	}

	@When("the member sorts the {roster} table by {string} in {direction} order")
	public void theMemberSortsTheTableBy(RosterName roster, String header, String direction) {
		sortRosterBy(roster, header, direction);
	}

	@Given("a member has sorted the {roster} table by {string} in {direction} order")
	public void aMemberHasSortedTheTableBy(RosterName roster, String header, String direction) {
		browser.open(OVERVIEW);
		sortRosterBy(roster, header, direction);
	}

	@Then("the {roster} table is sorted by {string} in {direction} order")
	public void theTableIsSortedBy(RosterName roster, String header, String direction) {
		assertThat(sortOf(roster)).isEqualTo(new Sort(header, direction));
	}

	private void sortRosterBy(RosterName roster, String header, String direction) {
		sortBy(() -> browser.clickTheColumnHeader(roster, header), () -> sortOf(roster), header, direction);
	}

	private Sort sortOf(RosterName roster) {
		return overview().roster(roster).sort();
	}

	private Overview overview() {
		return browser.read("read-overview.js", Overview.class);
	}

	private void sortLedgerBy(String header, String direction) {
		sortBy(() -> browser.clickTheLedgerColumnHeader(header), () -> ledger().sort(), header, direction);
	}

	private void sortBy(Runnable clickTheColumn, Supplier<Sort> currentSort, String header, String direction) {
		if (direction.equals(ASCENDING)) {
			clickTheColumn.run();
			assertThat(currentSort.get()).as("a fresh column's sort after a single click").isEqualTo(new Sort(header, ASCENDING));
			return;
		}
		for (int click = 0; click < MAX_CLICKS_TO_REACH_A_DIRECTION && !sortMatches(currentSort, header, direction); click++) {
			clickTheColumn.run();
		}
		assertThat(currentSort.get()).isEqualTo(new Sort(header, direction));
	}

	private static boolean sortMatches(Supplier<Sort> currentSort, String header, String direction) {
		Sort sort = currentSort.get();
		return sort != null && sort.equals(new Sort(header, direction));
	}

	private MemberLedger ledger() {
		return browser.read("read-ledger.js", MemberLedger.class);
	}
}
