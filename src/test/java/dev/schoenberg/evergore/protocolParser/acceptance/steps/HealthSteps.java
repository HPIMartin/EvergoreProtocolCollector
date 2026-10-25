package dev.schoenberg.evergore.protocolParser.acceptance.steps;

import io.cucumber.java.ParameterType;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import dev.schoenberg.evergore.protocolParser.acceptance.operator.AlteredLogins;
import dev.schoenberg.evergore.protocolParser.acceptance.operator.HealthFacts;
import dev.schoenberg.evergore.protocolParser.acceptance.operator.LoginRequests;
import dev.schoenberg.evergore.protocolParser.acceptance.world.Accounts;
import dev.schoenberg.evergore.protocolParser.acceptance.world.HealthReport;
import dev.schoenberg.evergore.protocolParser.acceptance.world.ScenarioTime;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.Login;

import static dev.schoenberg.evergore.protocolParser.acceptance.operator.FindingList.UNKNOWN_ITEMS;
import static dev.schoenberg.evergore.protocolParser.acceptance.operator.FindingList.WORTHLESS_ITEMS;
import static org.assertj.core.api.Assertions.assertThat;

public class HealthSteps {
	private static final String UNKNOWN_ITEM = "unknown item";
	private static final String DOWN = "DOWN";
	private static final int OK = 200;
	private static final int UNAVAILABLE = 503;

	private final HealthReport health;
	private final LoginRequests logins;
	private final Accounts accounts;
	private final ScenarioTime time;

	public HealthSteps(HealthReport health, LoginRequests logins, Accounts accounts, ScenarioTime time) {
		this.health = health;
		this.logins = logins;
		this.accounts = accounts;
		this.time = time;
	}

	@ParameterType("unknown item|item worth nothing")
	public String countedFinding(String finding) {
		return UNKNOWN_ITEM.equals(finding) ? "unknownItemCount" : "zeroValuedItemCount";
	}

	@When("the operator asks the service for its health without a login")
	public void theOperatorAsksForItsHealthWithoutALogin() {
		health.askWithoutALogin();
	}

	@When("the operator asks the service for its health with the admin key")
	public void theOperatorAsksForItsHealthWithTheAdminKey() {
		health.ask();
	}

	@When("a client logged in as {string} with the password {string} asks the service for its health")
	public void aClientLoggedInAsAsksForItsHealth(String name, String password) {
		health.askWithTheLogin(loginOf(name, password));
	}

	@When("a client logged in as {string} asks the service for its health with its login altered to claim the admin's role")
	public void aClientLoggedInAsAsksForItsHealthWithAnAlteredLogin(String avatar) {
		health.askWithTheLogin(AlteredLogins.claimingTheAdminsRole(loginOf(avatar, accounts.chosenBy(avatar))));
	}

	@When("a client asks the service for its health with a login that names {string} but is signed by someone other than the service")
	public void aClientAsksForItsHealthWithAForgedLogin(String avatar) {
		health.askWithTheLogin(AlteredLogins.signedBySomeoneOtherThanTheService(loginOf(avatar, accounts.chosenBy(avatar))));
	}

	@When("a client asks the service for its health with a login of {string} that has run its 7 days")
	public void aClientAsksForItsHealthWithAnEndedLogin(String avatar) {
		String login = loginOf(avatar, accounts.chosenBy(avatar));
		time.passes(Login.LIFETIME.plusSeconds(1));
		health.askWithTheLogin(login);
	}

	@Then("the operator finds the answer holding its verdict and nothing else")
	public void theOperatorFindsTheAnswerHoldingItsVerdictAndNothingElse() {
		assertThat(health.status()).as("the health report " + health).isNotEmpty();
		assertThat(health.namesBesidesTheVerdict()).as("what the health report holds besides its verdict: " + health).isEmpty();
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

	private String loginOf(String name, String password) {
		return LoginRequests.loginValueIn(logins.logIn(name, password));
	}

	private HealthFacts facts() {
		return new HealthFacts(health);
	}
}
