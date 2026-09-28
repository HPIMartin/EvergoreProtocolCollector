package dev.schoenberg.evergore.protocolParser.acceptance.steps;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Supplier;

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
	private static final String CREDIT_TOKEN = "--color-positive";
	private static final String DEBIT_TOKEN = "--color-negative";
	private static final String TEXT_TOKEN = "--color-text";

	private final MemberBrowser browser;

	public DashboardFiguresAndTimesSteps(MemberBrowser browser) {
		this.browser = browser;
	}

	@Given("the member's browser runs on New York time")
	public void theMembersBrowserRunsOnNewYorkTime() {
		browser.runsInTimeZone(NEW_YORK);
	}

	@Then("{word}'s {string} shows {int} {tone}")
	public void membersFigureShowsTone(String member, String header, int value, Tone tone) {
		Placed placed = rowOf(member);
		int column = columnOf(placed.headers(), header);
		String colour = browser.colourOfTheCellOf(member, column);

		assertThat(placed.row().cells().get(column)).isEqualTo(String.valueOf(value));
		assertToneIsShown(tone, colour, () -> browser.colourAroundTheCellOf(member, column), "the colour of " + member + "'s " + header);
	}

	private void assertToneIsShown(Tone tone, String colour, Supplier<String> colourAround, String subject) {
		assertThat(colour).as(subject).isEqualTo(expectedColourOf(tone, colourAround));
		if (tone == Tone.NEUTRAL) {
			assertThat(colourAround.get()).as(subject + ": the plain text colour around it").isEqualTo(browser.colourOfTheToken(TEXT_TOKEN));
		}
		assertTonesArePaintedApart(colourAround, subject);
	}

	private void assertTonesArePaintedApart(Supplier<String> colourAround, String subject) {
		String credit = expectedColourOf(Tone.CREDIT, colourAround);
		String debit = expectedColourOf(Tone.DEBIT, colourAround);
		String neutral = expectedColourOf(Tone.NEUTRAL, colourAround);

		assertThat(credit).as(subject + ": the credit colour against the neutral colour").isNotEqualTo(neutral);
		assertThat(debit).as(subject + ": the debit colour against the neutral colour").isNotEqualTo(neutral);
		assertThat(debit).as(subject + ": the debit colour against the credit colour").isNotEqualTo(credit);
	}

	private String expectedColourOf(Tone tone, Supplier<String> colourAround) {
		return switch (tone) {
			case CREDIT -> browser.colourOfTheToken(CREDIT_TOKEN);
			case DEBIT -> browser.colourOfTheToken(DEBIT_TOKEN);
			case NEUTRAL -> colourAround.get();
		};
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
