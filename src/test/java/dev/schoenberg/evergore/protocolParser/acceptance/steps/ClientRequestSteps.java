package dev.schoenberg.evergore.protocolParser.acceptance.steps;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.cucumber.java.ParameterType;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import dev.schoenberg.evergore.protocolParser.acceptance.operator.AlteredLogins;
import dev.schoenberg.evergore.protocolParser.acceptance.operator.BundledFiles;
import dev.schoenberg.evergore.protocolParser.acceptance.operator.ClientAnswer;
import dev.schoenberg.evergore.protocolParser.acceptance.operator.ClientTraffic;
import dev.schoenberg.evergore.protocolParser.acceptance.operator.LoginRequests;
import dev.schoenberg.evergore.protocolParser.acceptance.service.RunningService;
import dev.schoenberg.evergore.protocolParser.acceptance.world.Accounts;
import dev.schoenberg.evergore.protocolParser.acceptance.world.ScenarioTime;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.Login;

import static dev.schoenberg.evergore.protocolParser.acceptance.operator.ClientTraffic.LOGIN_PAGE;
import static dev.schoenberg.evergore.protocolParser.acceptance.operator.ClientTraffic.SERVED;
import static dev.schoenberg.evergore.protocolParser.acceptance.operator.ClientTraffic.TOO_MANY_REQUESTS;
import static dev.schoenberg.evergore.protocolParser.acceptance.steps.Pages.OVERVIEW;
import static org.assertj.core.api.Assertions.assertThat;

public class ClientRequestSteps {
	private static final String HEALTH_CHECK = "/health";
	private static final String USER_AGENT = "User-Agent";
	private static final int REQUESTS_WITHOUT_A_LOGIN_AT_ONCE = 10;
	private static final int LONG_ADDRESS = 5000;

	private final ClientTraffic traffic;
	private final LoginRequests logins;
	private final Accounts accounts;
	private final RunningService service;
	private final ScenarioTime time;
	private final ClientAnswer received;
	private int sentAtOnce;

	public ClientRequestSteps(ClientTraffic traffic, LoginRequests logins, Accounts accounts, RunningService service, ScenarioTime time, ClientAnswer received) {
		this.traffic = traffic;
		this.logins = logins;
		this.accounts = accounts;
		this.service = service;
		this.time = time;
		this.received = received;
	}

	@ParameterType("\\d+ seconds|\\d+ minutes|one minute")
	public Duration delay(String delay) {
		if ("one minute".equals(delay)) {
			return Duration.ofMinutes(1);
		}
		long amount = Long.parseLong(delay.split(" ")[0]);
		return delay.endsWith("minutes") ? Duration.ofMinutes(amount) : Duration.ofSeconds(amount);
	}

	@ParameterType("the login page|a file the login page loads|the health check|the overview|a page whose address is 5000 characters long")
	public String page(String page) {
		return switch (page) {
			case "the login page" -> LOGIN_PAGE;
			case "a file the login page loads" -> BundledFiles.aFileTheLoginPageLoads();
			case "the health check" -> HEALTH_CHECK;
			case "the overview" -> OVERVIEW;
			default -> LOGIN_PAGE + "a".repeat(LONG_ADDRESS - LOGIN_PAGE.length());
		};
	}

	@Given("a client without a login has sent {int} requests at once")
	public void aClientWithoutALoginHasSentRequestsAtOnce(int count) {
		traffic.startOffTheClocksGrid();
		sentAtOnce = count;

		assertThat(traffic.send(count, LOGIN_PAGE, Map.of())).as("the requests the client was allowed to send").hasSize(count).containsOnly(SERVED);
	}

	@Given("a client at the network address {string} was turned away for sending too many requests without a login")
	public void aClientAtWasTurnedAwayWithoutALogin(String address) {
		traffic.useClient(address, Optional.empty());
		turnAway(Map.of());
	}

	@Given("a client at the network address {string} with the browser {string} was turned away for sending too many requests without a login")
	public void aClientAtWithTheBrowserWasTurnedAwayWithoutALogin(String address, String browser) {
		traffic.useClient(address, Optional.empty());
		turnAway(Map.of(USER_AGENT, browser));
	}

	@When("the client sends one more request {delay} after its first")
	public void theClientSendsOneMoreRequestAfterItsFirst(Duration delay) {
		traffic.setTimeAfterTheFirstRequest(delay);
		traffic.send(1, traffic.addressWithTheClientsRights(), traffic.withTheClientsLogin(Map.of()));
	}

	@When("the client sends {int} requests {delay} after its first")
	public void theClientSendsRequestsAfterItsFirst(int count, Duration delay) {
		traffic.setTimeAfterTheFirstRequest(delay);
		traffic.send(count, LOGIN_PAGE, Map.of());
	}

	@When("a client without a login asks for the health check every {int} seconds for {int} minutes")
	public void aClientWithoutALoginAsksForTheHealthCheckEvery(int seconds, int minutes) {
		traffic.startOffTheClocksGrid();
		Duration every = Duration.ofSeconds(seconds);
		List<Integer> answered = new ArrayList<>();
		for (Duration since = Duration.ZERO; since.compareTo(Duration.ofMinutes(minutes)) < 0; since = since.plus(every)) {
			traffic.setTimeAfterTheFirstRequest(since);
			answered.addAll(traffic.send(1, HEALTH_CHECK, Map.of()));
		}
		traffic.rememberStatuses(answered);
	}

	@When("the client asks for {page} without a login {delay} after its first request")
	public void theClientAsksForAPageAfterItsFirstRequest(String page, Duration delay) {
		traffic.setTimeAfterTheFirstRequest(delay);
		traffic.send(1, page, Map.of());
	}

	@When("the client asks for a login as {string} with her password without a login {delay} after its first request")
	public void theClientAsksForALoginWithHerPassword(String name, Duration delay) {
		traffic.setTimeAfterTheFirstRequest(delay);
		received.remember(logins.logInFrom(traffic.client(), name, accounts.chosenBy(name)));
	}

	@When("the client asks for the health check with a login that names {string} but is signed by someone other than the service {delay} after its first request")
	public void theClientAsksForTheHealthCheckWithAForgedLogin(String name, Duration delay) {
		askForTheHealthCheckWith(AlteredLogins.signedBySomeoneOtherThanTheService(loginOf(name)), delay);
	}

	@When("the client asks for the health check with a login that names {string} and carries no signature at all {delay} after its first request")
	public void theClientAsksForTheHealthCheckWithALoginWithoutASignature(String name, Duration delay) {
		askForTheHealthCheckWith(AlteredLogins.withoutASignature(loginOf(name)), delay);
	}

	@When("the client asks for the health check with a login of {string} that has ended with her logging out {delay} after its first request")
	public void theClientAsksForTheHealthCheckWithALoggedOutLogin(String name, Duration delay) {
		String login = loginOf(name);
		logins.logOut(login);
		askForTheHealthCheckWith(login, delay);
	}

	@When("the client asks for the health check with a login of {string} that has run its 7 days {delay} after its first request")
	public void theClientAsksForTheHealthCheckWithAnEndedLogin(String name, Duration delay) {
		time.setTo(traffic.firstRequest().minus(Login.LIFETIME).minusSeconds(1));
		String login = loginOf(name);
		askForTheHealthCheckWith(login, delay);
	}

	@When("the client asks for the health check with a login of {string} from before a restart of the service {delay} after its first request")
	public void theClientAsksForTheHealthCheckWithALoginFromBeforeARestart(String name, Duration delay) {
		String login = loginOf(name);
		service.restart();
		time.setTo(traffic.firstRequest());
		traffic.send(sentAtOnce, LOGIN_PAGE, Map.of());
		askForTheHealthCheckWith(login, delay);
	}

	@When("the client asks for the health check with a login whose value is empty {delay} after its first request")
	public void theClientAsksForTheHealthCheckWithAnEmptyLogin(Duration delay) {
		askForTheHealthCheckWith("", delay);
	}

	@When("a client at the network address {string} asks for {page} without a login")
	public void aClientAtAsksForWithoutALogin(String address, String page) {
		traffic.useClient(address, Optional.empty());
		traffic.send(1, page, Map.of());
	}

	@When("a client at the network address {string} asks for {page} without a login with the browser {string}")
	public void aClientAtAsksForWithTheBrowser(String address, String page, String browser) {
		traffic.useClient(address, Optional.empty());
		traffic.send(1, page, Map.of(USER_AGENT, browser));
	}

	@When("a client at the network address {string} asks for {page} logged in as {string} with the browser {string}")
	public void aClientAtAsksForLoggedInWithTheBrowser(String address, String page, String name, String browser) {
		traffic.useClient(address, Optional.of(loginOf(name)));
		traffic.send(1, page, traffic.withTheClientsLogin(Map.of(USER_AGENT, browser)));
	}

	@When("the client asks for {page} without a login with the browser {string}")
	public void theClientAsksForWithTheBrowser(String page, String browser) {
		traffic.send(1, page, Map.of(USER_AGENT, browser));
	}

	@Then("the operator finds the client turned away for sending too many requests")
	public void theOperatorFindsTheClientTurnedAway() {
		assertThat(received.current().status()).isEqualTo(TOO_MANY_REQUESTS);
	}

	@Then("the operator finds the request served")
	public void theOperatorFindsTheRequestServed() {
		assertThat(received.current().status()).isEqualTo(SERVED);
	}

	@Then("the operator finds the first of them served and the second turned away for sending too many requests")
	public void theOperatorFindsTheFirstServedAndTheSecondTurnedAway() {
		assertThat(traffic.statuses()).isEqualTo(servedThenTurnedAway(1));
	}

	@Then("the operator finds the first {int} of them served and the {word} turned away for sending too many requests")
	public void theOperatorFindsTheFirstServedAndTheNextTurnedAway(int served, String ordinal) {
		assertThat(ordinal).isEqualTo(ordinalOf(served + 1));
		assertThat(traffic.statuses()).isEqualTo(servedThenTurnedAway(served));
	}

	@Then("the operator finds every one of its requests served")
	public void theOperatorFindsEveryOneOfItsRequestsServed() {
		assertThat(traffic.statuses()).isNotEmpty().containsOnly(SERVED);
	}

	private void turnAway(Map<String, String> headers) {
		traffic.startOffTheClocksGrid();

		assertThat(traffic.send(REQUESTS_WITHOUT_A_LOGIN_AT_ONCE, LOGIN_PAGE, headers)).as("the requests the client was allowed to send").containsOnly(SERVED);
		int beyond = traffic.send(1, LOGIN_PAGE, headers).getFirst();

		assertThat(beyond).as("the request beyond the allowed ones").isEqualTo(TOO_MANY_REQUESTS);
	}

	private void askForTheHealthCheckWith(String login, Duration delay) {
		traffic.setTimeAfterTheFirstRequest(delay);
		traffic.send(1, HEALTH_CHECK, LoginRequests.withTheLogin(Map.of(), login));
	}

	private String loginOf(String name) {
		return LoginRequests.loginValueIn(logins.logIn(name, accounts.chosenBy(name)));
	}

	private static List<Integer> servedThenTurnedAway(int served) {
		List<Integer> expected = new ArrayList<>(Collections.nCopies(served, SERVED));
		expected.add(TOO_MANY_REQUESTS);
		return expected;
	}

	private static String ordinalOf(int number) {
		int lastTwoDigits = number % 100;
		if (lastTwoDigits >= 11 && lastTwoDigits <= 13) {
			return number + "th";
		}
		return number + switch (number % 10) {
			case 1 -> "st";
			case 2 -> "nd";
			case 3 -> "rd";
			default -> "th";
		};
	}
}
