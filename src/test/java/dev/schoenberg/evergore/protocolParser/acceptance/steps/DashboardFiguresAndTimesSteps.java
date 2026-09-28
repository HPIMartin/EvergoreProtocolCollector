package dev.schoenberg.evergore.protocolParser.acceptance.steps;

import java.time.LocalDateTime;
import java.util.List;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;

import dev.schoenberg.evergore.protocolParser.acceptance.browser.MemberBrowser;
import dev.schoenberg.evergore.protocolParser.acceptance.browser.MemberLedger;
import dev.schoenberg.evergore.protocolParser.acceptance.browser.Overview;
import dev.schoenberg.evergore.protocolParser.acceptance.browser.Overview.Placed;

import static dev.schoenberg.evergore.protocolParser.acceptance.browser.MemberLedger.MINUTE_COLUMN;
import static dev.schoenberg.evergore.protocolParser.acceptance.world.GameProtocol.MINUTE;
import static org.assertj.core.api.Assertions.assertThat;

public class DashboardFiguresAndTimesSteps {
	private static final String NEW_YORK = "America/New_York";

	private final MemberBrowser browser;

	public DashboardFiguresAndTimesSteps(MemberBrowser browser) {
		this.browser = browser;
	}

	@Given("the member's browser runs on New York time")
	public void theMembersBrowserRunsOnNewYorkTime() {
		browser.runsInTimeZone(NEW_YORK);
	}

	@Then("the ledger shows a movement at {moment}")
	public void theLedgerShowsAMovementAt(LocalDateTime moment) {
		MemberLedger ledger = ledger();
		int column = ledger.columnOf(MINUTE_COLUMN);

		assertThat(ledger.rows()).extracting(row -> row.get(column)).contains(MINUTE.format(moment));
	}

	private MemberLedger ledger() {
		return browser.read("read-ledger.js", MemberLedger.class);
	}

	private Placed rowOf(String member) {
		return overview()
				.placedMemberRows()
				.stream()
				.filter(placed -> placed.row().cells().getFirst().equals(member))
				.findFirst()
				.orElseThrow(() -> new AssertionError("The overview lists no row for " + member));
	}

	private Overview overview() {
		return browser.read("read-overview.js", Overview.class);
	}

	private static int columnOf(List<String> headers, String header) {
		int column = headers.indexOf(header);
		assertThat(column).as("the overview's column " + header + " among " + headers).isNotNegative();
		return column;
	}
}
