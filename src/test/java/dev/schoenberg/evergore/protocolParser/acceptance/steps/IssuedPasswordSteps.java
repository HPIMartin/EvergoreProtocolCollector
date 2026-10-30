package dev.schoenberg.evergore.protocolParser.acceptance.steps;

import java.util.List;
import java.util.function.Supplier;

import io.cucumber.java.ParameterType;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import dev.schoenberg.evergore.protocolParser.acceptance.operator.Answer;
import dev.schoenberg.evergore.protocolParser.acceptance.operator.ClientAnswer;
import dev.schoenberg.evergore.protocolParser.acceptance.operator.LoginRequests;
import dev.schoenberg.evergore.protocolParser.acceptance.world.Accounts;
import dev.schoenberg.evergore.protocolParser.acceptance.world.ServiceLog;

import static org.assertj.core.api.Assertions.assertThat;

public class IssuedPasswordSteps {
	private static final String ADMIN = "admin";
	private static final int SERVED = 200;

	private final LoginRequests logins;
	private final ClientAnswer received;
	private final Accounts accounts;
	private final ServiceLog log;
	private List<String> logRead = List.of();

	public IssuedPasswordSteps(LoginRequests logins, ClientAnswer received, Accounts accounts, ServiceLog log) {
		this.logins = logins;
		this.received = received;
		this.accounts = accounts;
		this.log = log;
	}

	@ParameterType("the answer|the log")
	public Supplier<List<String>> whereLooked(String phrase) {
		return "the answer".equals(phrase) ? () -> List.of(bodyOfTheServedAnswer()) : () -> logRead;
	}

	@When("a client asks for the list of accounts with the admin key")
	public void aClientAsksForTheListOfAccountsWithTheAdminKey() {
		Answer loggedIn = logins.logIn(ADMIN, log.adminKeyNamed());

		received.remember(logins.listAccounts(LoginRequests.loginValueIn(loggedIn)));
	}

	@When("the operator reads the service's log")
	public void theOperatorReadsTheServicesLog() {
		logRead = List.copyOf(log.lines());
	}

	@Then("the operator finds the password the reset issued to {word} nowhere in {whereLooked}")
	public void theOperatorFindsThePasswordTheResetIssuedNowhereIn(String avatar, Supplier<List<String>> looked) {
		String issued = accounts.issuedTo(avatar);

		assertThat(looked.get()).isNotEmpty().noneMatch(text -> text.contains(issued));
	}

	private String bodyOfTheServedAnswer() {
		Answer answer = received.current();

		assertThat(answer.status()).as("the answer: " + answer.body()).isEqualTo(SERVED);
		return answer.body();
	}
}
