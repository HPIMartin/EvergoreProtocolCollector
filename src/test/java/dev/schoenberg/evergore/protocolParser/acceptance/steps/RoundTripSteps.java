package dev.schoenberg.evergore.protocolParser.acceptance.steps;

import io.cucumber.java.en.Then;

import dev.schoenberg.evergore.protocolParser.acceptance.browser.AdminPage;
import dev.schoenberg.evergore.protocolParser.acceptance.browser.MemberBrowser;

import static org.assertj.core.api.Assertions.assertThat;

public class RoundTripSteps {
	private static final String ROUND_TRIP_PREFIX = "Verdacht auf Warenkreislauf";
	private static final String ABSTENTION_PREFIX = "Nicht beurteilbar (Rezept ungelesen)";

	private final MemberBrowser browser;

	public RoundTripSteps(MemberBrowser browser) {
		this.browser = browser;
	}

	@Then("the admin page lists no suspected round trip")
	public void theAdminPageListsNoSuspectedRoundTrip() {
		assertThat(adminPage().messages()).noneMatch(message -> message.startsWith(ROUND_TRIP_PREFIX));
	}

	@Then("the admin page lists no pair as not judgeable")
	public void theAdminPageListsNoPairAsNotJudgeable() {
		assertThat(adminPage().messages()).noneMatch(message -> message.startsWith(ABSTENTION_PREFIX));
	}

	private AdminPage adminPage() {
		return browser.read("read-admin.js", AdminPage.class);
	}
}
