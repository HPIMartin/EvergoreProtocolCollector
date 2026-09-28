package dev.schoenberg.evergore.protocolParser.acceptance.steps;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import dev.schoenberg.evergore.protocolParser.acceptance.browser.AdminPage;
import dev.schoenberg.evergore.protocolParser.acceptance.browser.MemberBrowser;
import dev.schoenberg.evergore.protocolParser.acceptance.world.HealthReport;

import static org.assertj.core.api.Assertions.assertThat;

public class RoundTripSteps {
	private static final String ROUND_TRIP_PREFIX = "Verdacht auf Warenkreislauf";
	private static final String ABSTENTION_PREFIX = "Nicht beurteilbar (Rezept ungelesen)";

	private final MemberBrowser browser;
	private final HealthReport health;

	public RoundTripSteps(MemberBrowser browser, HealthReport health) {
		this.browser = browser;
		this.health = health;
	}

	@When("the operator asks the service for its health")
	public void theOperatorAsksTheServiceForItsHealth() {
		health.ask();
	}

	@Then("the admin page lists no suspected round trip")
	public void theAdminPageListsNoSuspectedRoundTrip() {
		assertThat(adminPage().messages()).noneMatch(message -> message.startsWith(ROUND_TRIP_PREFIX));
	}

	@Then("the admin page lists no pair as not judgeable")
	public void theAdminPageListsNoPairAsNotJudgeable() {
		assertThat(adminPage().messages()).noneMatch(message -> message.startsWith(ABSTENTION_PREFIX));
	}

	@Then("{surface} names the suspected round trip {string}")
	public void namesTheSuspectedRoundTrip(Surface surface, String roundTrip) {
		assertThat(roundTripFound(surface, roundTrip)).as(surface + " naming the suspected round trip " + roundTrip).isTrue();
	}

	@Then("{surface} names the pair not judgeable {string}")
	public void namesThePairNotJudgeable(Surface surface, String pair) {
		assertThat(abstentionFound(surface, pair)).as(surface + " naming the pair not judgeable " + pair).isTrue();
	}

	private boolean roundTripFound(Surface surface, String roundTrip) {
		return switch (surface) {
			case ADMIN_PAGE -> adminPage().messages().stream().anyMatch(message -> message.startsWith(ROUND_TRIP_PREFIX) && message.contains(roundTrip));
			case HEALTH_REPORT -> health.roundTripMessages().contains(roundTrip);
		};
	}

	private boolean abstentionFound(Surface surface, String pair) {
		return switch (surface) {
			case ADMIN_PAGE -> adminPage().messages().stream().anyMatch(message -> message.startsWith(ABSTENTION_PREFIX) && message.contains(pair));
			case HEALTH_REPORT -> health.abstentionMessages().contains(pair);
		};
	}

	private AdminPage adminPage() {
		return browser.read("read-admin.js", AdminPage.class);
	}
}
