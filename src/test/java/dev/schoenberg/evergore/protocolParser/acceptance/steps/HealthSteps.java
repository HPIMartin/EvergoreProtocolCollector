package dev.schoenberg.evergore.protocolParser.acceptance.steps;

import io.cucumber.java.ParameterType;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import dev.schoenberg.evergore.protocolParser.acceptance.operator.HealthFacts;
import dev.schoenberg.evergore.protocolParser.acceptance.world.HealthReport;

import static dev.schoenberg.evergore.protocolParser.acceptance.operator.FindingList.UNKNOWN_ITEMS;
import static dev.schoenberg.evergore.protocolParser.acceptance.operator.FindingList.WORTHLESS_ITEMS;
import static org.assertj.core.api.Assertions.assertThat;

public class HealthSteps {
	private static final String UNKNOWN_ITEM = "unknown item";
	private static final String DOWN = "DOWN";
	private static final int OK = 200;
	private static final int UNAVAILABLE = 503;

	private final HealthReport health;

	public HealthSteps(HealthReport health) {
		this.health = health;
	}

	@ParameterType("unknown item|item worth nothing")
	public String countedFinding(String finding) {
		return UNKNOWN_ITEM.equals(finding) ? "unknownItemCount" : "zeroValuedItemCount";
	}

	@When("the operator asks the service for its health without a token")
	public void theOperatorAsksForItsHealthWithoutAToken() {
		health.askWithoutToken();
	}

	@Then("the service reports its health as {string}")
	public void theServiceReportsItsHealthAs(String status) {
		assertThat(health.status()).as("the health report " + health).isEqualTo(status);
		assertThat(health.httpStatus()).as("the health check's answer").isEqualTo(DOWN.equals(status) ? UNAVAILABLE : OK);
	}

	@Then("the health report names {string} as an unknown item")
	public void theHealthReportNamesAnUnknownItem(String item) {
		assertThat(facts().list(UNKNOWN_ITEMS)).hasValueSatisfying(names -> assertThat(names).contains(item));
	}

	@Then("the health report names {string} as an item worth nothing")
	public void theHealthReportNamesAnItemWorthNothing(String item) {
		assertThat(facts().list(WORTHLESS_ITEMS)).hasValueSatisfying(names -> assertThat(names).contains(item));
	}

	@Then("the health report counts {int} {countedFinding}")
	public void theHealthReportCounts(int count, String countKey) {
		assertThat(facts().count(countKey)).contains(count);
	}

	private HealthFacts facts() {
		return new HealthFacts(health);
	}
}
