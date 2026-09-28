package dev.schoenberg.evergore.protocolParser.acceptance.steps;

import io.cucumber.java.en.When;

import dev.schoenberg.evergore.protocolParser.acceptance.browser.MemberBrowser;
import dev.schoenberg.evergore.protocolParser.acceptance.browser.Overview;
import dev.schoenberg.evergore.protocolParser.acceptance.browser.RosterName;
import dev.schoenberg.evergore.protocolParser.acceptance.browser.Sort;

import static org.assertj.core.api.Assertions.assertThat;

public class DashboardSortingSteps {
	private static final int MAX_CLICKS_TO_REACH_A_DIRECTION = 2;
	private static final String ASCENDING = "ascending";

	private final MemberBrowser browser;

	public DashboardSortingSteps(MemberBrowser browser) {
		this.browser = browser;
	}

	@When("the member sorts the {roster} table by {string} in {direction} order")
	public void theMemberSortsTheTableBy(RosterName roster, String header, String direction) {
		sortRosterBy(roster, header, direction);
	}

	private void sortRosterBy(RosterName roster, String header, String direction) {
		if (direction.equals(ASCENDING)) {
			sortAFreshColumnAscendingWithOneClick(roster, header);
			return;
		}
		for (int click = 0; click < MAX_CLICKS_TO_REACH_A_DIRECTION && !sortMatches(roster, header, direction); click++) {
			browser.clickTheColumnHeader(roster, header);
		}
		assertThat(sortOf(roster)).isEqualTo(new Sort(header, direction));
	}

	private void sortAFreshColumnAscendingWithOneClick(RosterName roster, String header) {
		browser.clickTheColumnHeader(roster, header);
		assertThat(sortOf(roster)).as("a fresh column's sort after a single click").isEqualTo(new Sort(header, ASCENDING));
	}

	private boolean sortMatches(RosterName roster, String header, String direction) {
		Sort sort = sortOf(roster);
		return sort != null && sort.equals(new Sort(header, direction));
	}

	private Sort sortOf(RosterName roster) {
		return overview().roster(roster).sort();
	}

	private Overview overview() {
		return browser.read("read-overview.js", Overview.class);
	}
}
