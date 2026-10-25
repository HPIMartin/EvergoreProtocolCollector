package dev.schoenberg.evergore.protocolParser.acceptance.steps;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;

import dev.schoenberg.evergore.protocolParser.acceptance.operator.Answer;
import dev.schoenberg.evergore.protocolParser.acceptance.operator.ClientTraffic;
import dev.schoenberg.evergore.protocolParser.acceptance.operator.LoginRequests;
import dev.schoenberg.evergore.protocolParser.acceptance.world.Accounts;

import static dev.schoenberg.evergore.protocolParser.acceptance.operator.ClientTraffic.LOGIN_PAGE;
import static dev.schoenberg.evergore.protocolParser.acceptance.operator.ClientTraffic.OVERVIEWS_FIGURES;
import static dev.schoenberg.evergore.protocolParser.acceptance.operator.ClientTraffic.SERVED;
import static dev.schoenberg.evergore.protocolParser.acceptance.operator.ClientTraffic.TOO_MANY_REQUESTS;
import static org.assertj.core.api.Assertions.assertThat;

public class ClientWithLoginSteps {
	private static final String A_CLIENT = "198.51.100.1";
	private static final int REQUESTS_WITH_A_LOGIN_IN_A_COUNT = 30;

	private final ClientTraffic traffic;
	private final LoginRequests logins;
	private final Accounts accounts;

	public ClientWithLoginSteps(ClientTraffic traffic, LoginRequests logins, Accounts accounts) {
		this.traffic = traffic;
		this.logins = logins;
		this.accounts = accounts;
	}

	@Given("a client logged in as {string} has sent {int} requests within its first second")
	public void aClientLoggedInHasSentRequestsWithinItsFirstSecond(String name, int count) {
		aClientAtLoggedInHasSentRequestsWithinItsFirstSecond(A_CLIENT, name, count);
	}

	@When("a client logged in as {string} sends {int} requests within its first second")
	public void aClientLoggedInSendsRequestsWithinItsFirstSecond(String name, int count) {
		startAs(A_CLIENT, name);
		sendWithTheLogin(count);
	}

	@Given("a client logged in as {string} was turned away for sending too many requests")
	public void aClientLoggedInWasTurnedAway(String name) {
		aClientAtLoggedInWasTurnedAway(A_CLIENT, name);
	}

	@Given("a client at the network address {string} logged in as {string} was turned away for sending too many requests")
	public void aClientAtLoggedInWasTurnedAway(String address, String name) {
		startAs(address, name);

		assertThat(sendWithTheLogin(REQUESTS_WITH_A_LOGIN_IN_A_COUNT)).as("the requests the client was allowed to send").containsOnly(SERVED);
		int beyond = sendWithTheLogin(1).getFirst();

		assertThat(beyond).as("the request beyond the allowed ones").isEqualTo(TOO_MANY_REQUESTS);
		traffic.rememberTheClientWasTurnedAwayNow();
	}

	@Given("a client at the network address {string} logged in as {string} has sent {int} requests within its first second")
	public void aClientAtLoggedInHasSentRequestsWithinItsFirstSecond(String address, String name, int count) {
		startAs(address, name);

		assertThat(sendWithTheLogin(count)).as("the requests the client was allowed to send").hasSize(count).containsOnly(SERVED);
	}

	@Given("a client at the network address {string} logged in as {string} has sent a request")
	public void aClientAtLoggedInHasSentARequest(String address, String name) {
		aClientAtLoggedInHasSentRequestsWithinItsFirstSecond(address, name, 1);
	}

	@Given("a client at the network address {string} logged in as {string} has sent a request within that second")
	public void aClientAtLoggedInHasSentARequestWithinThatSecond(String address, String name) {
		aClientAtLoggedInHasSentRequestsWithinThatSecond(address, name, 1);
	}

	@Given("a client at the network address {string} logged in as {string} has sent {int} requests within that second")
	public void aClientAtLoggedInHasSentRequestsWithinThatSecond(String address, String name, int count) {
		traffic.useClient(address, Optional.of(loginOf(name)));

		assertThat(sendWithTheLogin(count)).as("the requests the client was allowed to send").hasSize(count).containsOnly(SERVED);
	}

	@Given("a client at the network address {string} logged in as {string} has sent {int} requests within that second, the last of them turned away for sending too many requests")
	public void aClientAtLoggedInHasSentRequestsTheLastTurnedAway(String address, String name, int count) {
		traffic.useClient(address, Optional.of(loginOf(name)));

		List<Integer> statuses = sendWithTheLogin(count);

		assertThat(statuses.subList(0, count - 1)).as("the requests before the last").containsOnly(SERVED);
		assertThat(statuses.getLast()).as("the last request").isEqualTo(TOO_MANY_REQUESTS);
	}

	@Given("a client at the network address {string} logged in as {string} has sent {int} requests {delay} after the first")
	public void aClientAtLoggedInHasSentRequestsAfterTheFirst(String address, String name, int count, Duration delay) {
		traffic.useClient(address, Optional.of(loginOf(name)));
		traffic.setTimeAfterTheFirstRequest(delay);

		assertThat(sendWithTheLogin(count)).as("the requests the client was allowed to send").hasSize(count).containsOnly(SERVED);
	}

	@Given("a client at the network address {string} logged in as {string} has sent {int} requests {delay} after it was turned away")
	public void aClientAtLoggedInHasSentRequestsAfterItWasTurnedAway(String address, String name, int count, Duration delay) {
		traffic.useClient(address, Optional.of(loginOf(name)));
		traffic.setTimeAfterTheClientWasTurnedAway(delay);

		assertThat(sendWithTheLogin(count)).as("the requests the client was allowed to send").hasSize(count).containsOnly(SERVED);
	}

	@Given("a client at the network address {string} has logged in as {string}")
	public void aClientAtHasLoggedIn(String address, String name) {
		traffic.startOffTheClocksGrid();
		Answer answer = logins.logInFrom(address, name, accounts.chosenBy(name));

		assertThat(answer.status()).as("the login: " + answer.body()).isEqualTo(SERVED);
		traffic.useClient(address, Optional.of(LoginRequests.loginValueIn(answer)));
	}

	@Given("the client has sent {int} requests without a login within its first second")
	public void theClientHasSentRequestsWithoutALoginWithinItsFirstSecond(int count) {
		assertThat(sendWithoutALogin(count)).as("the requests the client was allowed to send").hasSize(count).containsOnly(SERVED);
	}

	@Given("the client has sent {int} requests without a login within that second")
	public void theClientHasSentRequestsWithoutALoginWithinThatSecond(int count) {
		assertThat(sendWithoutALogin(count)).as("the requests the client was allowed to send").hasSize(count).containsOnly(SERVED);
	}

	@When("the client sends a request {delay} after it was turned away")
	public void theClientSendsARequestAfterItWasTurnedAway(Duration delay) {
		traffic.setTimeAfterTheClientWasTurnedAway(delay);
		sendWithTheLogin(1);
	}

	@When("the client asks for the overview's figures with its login {delay} after its first request")
	public void theClientAsksForTheOverviewsFiguresWithItsLogin(Duration delay) {
		traffic.setTimeAfterTheFirstRequest(delay);
		sendWithTheLogin(1);
	}

	@When("a client at the network address {string} asks for the login page without a login {delay} after the first was turned away")
	public void aClientAtAsksForTheLoginPageAfterTheFirstWasTurnedAway(String address, Duration delay) {
		traffic.useClient(address, Optional.empty());
		traffic.setTimeAfterTheClientWasTurnedAway(delay);
		traffic.send(1, LOGIN_PAGE, Map.of());
	}

	@When("a client at the network address {string} asks for the login page without a login {delay} after the first request")
	public void aClientAtAsksForTheLoginPageAfterTheFirstRequest(String address, Duration delay) {
		traffic.useClient(address, Optional.empty());
		traffic.setTimeAfterTheFirstRequest(delay);
		traffic.send(1, LOGIN_PAGE, Map.of());
	}

	@When("a client at the network address {string} logged in as {string} sends a request {delay} after the first was turned away")
	public void aClientAtLoggedInSendsARequestAfterTheFirstWasTurnedAway(String address, String name, Duration delay) {
		traffic.useClient(address, Optional.of(loginOf(name)));
		traffic.setTimeAfterTheClientWasTurnedAway(delay);
		sendWithTheLogin(1);
	}

	@When("a client at the network address {string} logged in as {string} sends a request {delay} after the first")
	public void aClientAtLoggedInSendsARequestAfterTheFirst(String address, String name, Duration delay) {
		traffic.useClient(address, Optional.of(loginOf(name)));
		traffic.setTimeAfterTheFirstRequest(delay);
		sendWithTheLogin(1);
	}

	@When("a client at the network address {string} logged in as {string} sends a request within that second")
	public void aClientAtLoggedInSendsARequestWithinThatSecond(String address, String name) {
		traffic.useClient(address, Optional.of(loginOf(name)));
		sendWithTheLogin(1);
	}

	private void startAs(String address, String name) {
		traffic.startOffTheClocksGrid();
		traffic.useClient(address, Optional.of(loginOf(name)));
	}

	private List<Integer> sendWithTheLogin(int count) {
		return traffic.send(count, OVERVIEWS_FIGURES, traffic.withTheClientsLogin(Map.of()));
	}

	private List<Integer> sendWithoutALogin(int count) {
		return traffic.send(count, LOGIN_PAGE, Map.of());
	}

	private String loginOf(String name) {
		return LoginRequests.loginValueIn(logins.logIn(name, accounts.chosenBy(name)));
	}
}
