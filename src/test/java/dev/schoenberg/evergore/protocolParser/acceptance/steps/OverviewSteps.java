package dev.schoenberg.evergore.protocolParser.acceptance.steps;

import java.util.List;
import java.util.Optional;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import dev.schoenberg.evergore.protocolParser.acceptance.browser.MemberBrowser;
import dev.schoenberg.evergore.protocolParser.acceptance.browser.Overview;
import dev.schoenberg.evergore.protocolParser.acceptance.browser.Overview.Placed;
import dev.schoenberg.evergore.protocolParser.acceptance.browser.Overview.Row;
import dev.schoenberg.evergore.protocolParser.acceptance.browser.Overview.Stat;
import dev.schoenberg.evergore.protocolParser.acceptance.browser.RosterName;
import dev.schoenberg.evergore.protocolParser.acceptance.world.Guild;

import static dev.schoenberg.evergore.protocolParser.acceptance.steps.Pages.OVERVIEW;
import static org.assertj.core.api.Assertions.assertThat;

public class OverviewSteps {
	private static final String GUILD_ROW = "Gilde";
	private static final String NO_FIGURE = "–";
	private static final String NAME_SEPARATOR = ", ";
	private static final String FIGURE_HEADER_READS = "[...document.querySelectorAll('[data-testid=active-roster] [data-testid=column-label]')]"
			+ ".some((label) => label.textContent.trim() === arguments[0])";

	private final MemberBrowser browser;
	private final Guild guild;

	public OverviewSteps(MemberBrowser browser, Guild guild) {
		this.browser = browser;
		this.guild = guild;
	}

	@When("a member opens the overview")
	public void aMemberOpensTheOverview() {
		browser.open(OVERVIEW);
	}

	@Given("a member has opened the overview")
	public void aMemberHasOpenedTheOverview() {
		browser.open(OVERVIEW);
	}

	@When("the member switches the last column to {string}")
	public void theMemberSwitchesTheLastColumnTo(String figure) {
		switchTheLastColumnTo(figure);
	}

	@Given("a member has switched the overview's last column to {string}")
	public void aMemberHasSwitchedTheLastColumnTo(String figure) {
		browser.open(OVERVIEW);
		switchTheLastColumnTo(figure);
	}

	@When("the member reloads the overview")
	public void theMemberReloadsTheOverview() {
		browser.reload();
	}

	@When("the member follows {string} back from the bank ledger of {string}")
	public void theMemberFollowsBackFromTheBankLedgerOf(String link, String member) {
		browser.follow(member);
		browser.follow(link);
	}

	@Then("the {roster} table lists {string}")
	public void theTableLists(RosterName roster, String members) {
		Overview overview = browser.overview();

		assertThat(namesIn(overview.roster(roster).rows())).isEqualTo(List.of(members.split(NAME_SEPARATOR)));
	}

	@Then("{string} stands in the {roster} table")
	public void standsInTheTable(String member, RosterName roster) {
		Overview overview = browser.overview();

		assertThat(namesIn(overview.roster(roster).rows())).contains(member);
	}

	@Then("the {roster} table's caption reads {string}")
	public void theTablesCaptionReads(RosterName roster, String caption) {
		Overview overview = browser.overview();

		assertThat(overview.roster(roster).caption()).isEqualTo(caption);
	}

	@Then("the {roster} table says {string}")
	public void theTableSays(RosterName roster, String message) {
		Overview overview = browser.overview();

		assertThat(overview.roster(roster).emptyMessage()).isEqualTo(message);
	}

	@Then("the {roster} table ends with the guild row")
	public void theTableEndsWithTheGuildRow(RosterName roster) {
		Overview overview = browser.overview();

		Row total = overview.roster(roster).total();
		assertThat(total).isNotNull();
		assertThat(total.cells().getFirst()).isEqualTo(GUILD_ROW);
	}

	@Then("the {roster} table has no guild row")
	public void theTableHasNoGuildRow(RosterName roster) {
		Overview overview = browser.overview();

		assertThat(overview.roster(roster).total()).isNull();
	}

	@Then("the overview shows no guild row")
	public void theOverviewShowsNoGuildRow() {
		Overview overview = browser.overview();

		assertThat(overview.placedGuildRows()).isEmpty();
	}

	@Then("the overview shows:")
	public void theOverviewShows(DataTable expected) {
		Overview overview = browser.overview();

		assertThat(projected(rowsNamedIn(overview.placedMemberRows(), expected), expected)).isEqualTo(expected.cells().subList(1, expected.height()));
	}

	@Then("the guild row shows:")
	public void theGuildRowShows(DataTable expected) {
		Overview overview = browser.overview();

		assertThat(projected(overview.placedGuildRows(), expected)).isEqualTo(expected.cells().subList(1, expected.height()));
	}

	@Then("the overview lists {string}")
	public void theOverviewLists(String members) {
		Overview overview = browser.overview();

		assertThat(namesIn(overview.memberRows())).isEqualTo(List.of(members.split(NAME_SEPARATOR)));
	}

	@Then("the overview lists {int} members")
	public void theOverviewListsMembers(int count) {
		Overview overview = browser.overview();

		assertThat(overview.memberRows()).hasSize(count);
	}

	@Then("the member last in German alphabetical order is not listed")
	public void theMemberLastInGermanOrderIsNotListed() {
		Overview overview = browser.overview();

		assertThat(namesIn(overview.memberRows())).doesNotContain(guild.inGermanOrder().getLast());
	}

	@Then("{word}'s {string} is {word}")
	public void figureIs(String member, String header, String figure) {
		Placed row = browser.overview().rowOf(member);
		int column = row.columnOf(header);

		assertThat(row.row().cells().get(column)).isEqualTo(figure);
	}

	@Then("the guild's position reads:")
	public void theGuildsPositionReads(DataTable expected) {
		List<Stat> position = browser.overview().position();

		List<String> shown = expected.row(0).stream().map(label -> valueOf(position, label)).toList();
		assertThat(shown).isEqualTo(expected.row(1));
	}

	@Then("the page says {string}")
	public void thePageSays(String message) {
		Overview overview = browser.overview();

		assertThat(overview.messages()).contains(message);
	}

	@Then("the last column reads {string}")
	public void theLastColumnReads(String figure) {
		Overview overview = browser.overview();

		assertThat(overview.active().figureHeader()).isEqualTo(figure);
	}

	@Then("{word}'s {string} shows no figure, noted {string}")
	public void showsNoFigureNoted(String member, String header, String note) {
		Placed row = browser.overview().rowOf(member);
		int column = row.columnOf(header);

		Optional<String> shown = browser.noteShownInTheCellOf(member, column);
		assertNoFigureNoted(row, column, note, shown);
	}

	@Then("the guild row's {string} shows no figure, noted {string}")
	public void theGuildRowShowsNoFigureNoted(String header, String note) {
		Placed row = guildRowOf(browser.overview());
		int column = row.columnOf(header);

		Optional<String> shown = browser.noteShownInTheGuildRowsCell(column);
		assertNoFigureNoted(row, column, note, shown);
	}

	@Then("{word}'s row is marked {string}")
	public void rowIsMarked(String member, String mark) {
		Placed row = browser.overview().rowOf(member);

		Optional<String> shown = browser.markShownOnTheRowOf(member);
		assertThat(row.row().mark()).isEqualTo(mark);
		assertThat(shown).contains(mark);
	}

	@Then("{word}'s row carries no mark")
	public void rowCarriesNoMark(String member) {
		Placed row = browser.overview().rowOf(member);

		assertThat(row.row().mark()).isNull();
	}

	@Then("the guild row is marked {string}")
	public void theGuildRowIsMarked(String mark) {
		Placed row = guildRowOf(browser.overview());

		Optional<String> shown = browser.markShownOnTheGuildRow();
		assertThat(row.row().mark()).isEqualTo(mark);
		assertThat(shown).contains(mark);
	}

	@Then("the guild row carries no mark")
	public void theGuildRowCarriesNoMark() {
		Placed row = guildRowOf(browser.overview());

		assertThat(row.row().mark()).isNull();
	}

	private void switchTheLastColumnTo(String figure) {
		browser.choose(figure);
		browser.awaitUntil(FIGURE_HEADER_READS, figure);
	}

	private static void assertNoFigureNoted(Placed row, int column, String note, Optional<String> shown) {
		assertThat(row.row().cells().get(column)).isEqualTo(NO_FIGURE);
		assertThat(row.row().notes().get(column)).isEqualTo(note);
		assertThat(shown).contains(note);
	}

	private static Placed guildRowOf(Overview overview) {
		return overview.placedGuildRows().stream().findFirst().orElseThrow(() -> new AssertionError("The overview shows no guild row"));
	}

	private static List<Placed> rowsNamedIn(List<Placed> rows, DataTable expected) {
		List<String> named = expected.column(0).subList(1, expected.height());
		return rows.stream().filter(placed -> named.contains(placed.row().cells().getFirst())).toList();
	}

	private static List<String> namesIn(List<Row> rows) {
		return rows.stream().map(row -> row.cells().getFirst()).toList();
	}

	private static List<List<String>> projected(List<Placed> rows, DataTable expected) {
		return rows.stream().map(placed -> expected.row(0).stream().map(header -> placed.row().cells().get(placed.columnOf(header))).toList()).toList();
	}

	private static String valueOf(List<Stat> position, String label) {
		return position
				.stream()
				.filter(stat -> stat.label().equals(label))
				.map(Stat::value)
				.findFirst()
				.orElseThrow(() -> new AssertionError("The guild's position shows no figure labelled " + label));
	}
}
