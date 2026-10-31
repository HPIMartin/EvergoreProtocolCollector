package dev.schoenberg.evergore.protocolParser.acceptance.steps;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import dev.schoenberg.evergore.protocolParser.acceptance.operator.Answer;
import dev.schoenberg.evergore.protocolParser.acceptance.operator.ClientAnswer;
import dev.schoenberg.evergore.protocolParser.acceptance.operator.LoginRequests;
import dev.schoenberg.evergore.protocolParser.acceptance.world.ServiceLog;

import static org.assertj.core.api.Assertions.assertThat;

public class RoleChangeSteps {
	private static final String ADMIN = "admin";
	private static final int SERVED = 200;

	private final LoginRequests logins;
	private final ClientAnswer received;
	private final ServiceLog log;

	public RoleChangeSteps(LoginRequests logins, ClientAnswer received, ServiceLog log) {
		this.logins = logins;
		this.received = received;
		this.log = log;
	}

	@When("the operator asks for the list of accounts with the admin key and reads the service's log")
	public void theOperatorAsksForTheListOfAccountsWithTheAdminKey() {
		received.remember(listAskedForWithTheAdminKey());
	}

	@Then("the operator finds that list giving {string} the role {string}")
	public void theOperatorFindsThatListGivingTheRole(String name, String role) {
		assertThat(LoginRequests.roleIn(servedAnswer(), name)).isEqualTo(role);
	}

	@Then("the operator finds the list of accounts, asked for with the admin key, giving {string} the role {string}")
	public void theOperatorFindsTheListAskedForWithTheAdminKeyGivingTheRole(String name, String role) {
		received.remember(listAskedForWithTheAdminKey());

		assertThat(LoginRequests.roleIn(servedAnswer(), name)).isEqualTo(role);
	}

	@Then("the operator finds no line {string} in the service's log")
	public void theOperatorFindsNoLineInTheServicesLog(String start) {
		assertThat(log.lines()).noneMatch(logged -> logged.startsWith(start));
	}

	private Answer listAskedForWithTheAdminKey() {
		Answer loggedIn = logins.logIn(ADMIN, log.adminKeyNamed());

		return logins.listAccounts(LoginRequests.loginValueIn(loggedIn));
	}

	private Answer servedAnswer() {
		Answer accounts = received.current();

		assertThat(accounts.status()).as("the list of accounts: " + accounts.body()).isEqualTo(SERVED);
		return accounts;
	}
}
