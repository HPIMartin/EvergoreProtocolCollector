package dev.schoenberg.evergore.protocolParser.acceptance.steps;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.cucumber.java.ParameterType;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import dev.schoenberg.evergore.protocolParser.acceptance.operator.Answer;
import dev.schoenberg.evergore.protocolParser.acceptance.operator.ServiceRequests;
import dev.schoenberg.evergore.protocolParser.acceptance.service.RunningService;
import dev.schoenberg.evergore.protocolParser.acceptance.world.ScenarioTime;
import dev.schoenberg.evergore.protocolParser.rest.filter.AcceptanceClientIp;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

public class ClientRequestSteps {
	private static final String START_PAGE = "/";
	private static final String A_CLIENT = "198.51.100.1";
	private static final String ANOTHER_CLIENT = "198.51.100.2";
	private static final String USER_AGENT = "User-Agent";
	private static final int ALLOWED_REQUESTS = 30;
	private static final int SERVED = 200;
	private static final int TOO_MANY_REQUESTS = 429;
	private static final int LONG_ADDRESS = 5000;
	private static final Duration OFF_THE_GRID = Duration.ofMillis(7_250);
	private static final Duration BETWEEN_REQUESTS = Duration.ofMillis(30);
	private static final Duration LATER_IN_THE_COUNT = Duration.ofSeconds(4);
	private static final Pattern DASHBOARD_FILE = Pattern.compile("\"(/assets/[^\"]+)\"");

	private final ServiceRequests requests;
	private final RunningService service;
	private final ScenarioTime time;
	private String client = A_CLIENT;
	private Instant firstRequest;
	private Instant turnedAway;
	private Answer answer;

	public ClientRequestSteps(ServiceRequests requests, RunningService service, ScenarioTime time) {
		this.requests = requests;
		this.service = service;
		this.time = time;
	}

	@ParameterType("\\d+ seconds|one minute")
	public Duration delay(String delay) {
		return "one minute".equals(delay) ? Duration.ofMinutes(1) : Duration.ofSeconds(Long.parseLong(delay.split(" ")[0]));
	}

	@ParameterType("the dashboard's start page|a file the dashboard loads|the health check|the overview|a page whose address is 5000 characters long")
	public String page(String page) {
		return switch (page) {
			case "the dashboard's start page" -> START_PAGE;
			case "a file the dashboard loads" -> aFileTheDashboardLoads();
			case "the health check" -> "/health";
			case "the overview" -> "/overview";
			default -> START_PAGE + "a".repeat(LONG_ADDRESS - START_PAGE.length());
		};
	}

	@Given("a client has sent 30 requests within its first second")
	public void aClientHasSentTheAllowedRequests() {
		startOffTheClocksGrid();
		for (int request = 0; request < ALLOWED_REQUESTS; request++) {
			int status = send(START_PAGE, Map.of()).status();

			assertThat(status).as("request %d of the allowed ones", request + 1).isEqualTo(SERVED);
			time.passes(BETWEEN_REQUESTS);
		}
	}

	@Given("a client was turned away for sending too many requests")
	public void aClientWasTurnedAway() {
		turnAway(Map.of());
	}

	@Given("a client at the network address {string} with the browser {string} was turned away for sending too many requests")
	public void aClientAtWithTheBrowserWasTurnedAway(String address, String browser) {
		client = address;
		turnAway(Map.of(USER_AGENT, browser));
	}

	@When("the client sends one more request {delay} after its first")
	public void theClientSendsOneMoreRequestAfterItsFirst(Duration delay) {
		time.setTo(firstRequest.plus(delay));
		answer = send(START_PAGE, Map.of());
	}

	@When("the client sends a request {delay} after it was turned away")
	public void theClientSendsARequestAfterItWasTurnedAway(Duration delay) {
		time.setTo(turnedAway.plus(delay));
		answer = send(START_PAGE, Map.of());
	}

	@When("another client sends a request")
	public void anotherClientSendsARequest() {
		client = ANOTHER_CLIENT;
		answer = send(START_PAGE, Map.of());
	}

	@When("the client asks for {page} without a token 5 seconds after its first request")
	public void theClientAsksForAPageFiveSecondsAfterItsFirst(String page) {
		time.setTo(firstRequest.plus(Duration.ofSeconds(5)));
		answer = send(page, Map.of());
	}

	@When("a client at the network address {string} asks for {page} without a token with the browser {string}")
	public void aClientAtAsksForWithTheBrowser(String address, String page, String browser) {
		client = address;
		answer = send(page, Map.of(USER_AGENT, browser));
	}

	@When("the client asks for {page} without a token with the browser {string}")
	public void theClientAsksForWithTheBrowser(String page, String browser) {
		answer = send(page, Map.of(USER_AGENT, browser));
	}

	@Then("the operator finds the client turned away for sending too many requests")
	public void theOperatorFindsTheClientTurnedAway() {
		assertThat(answer.status()).isEqualTo(TOO_MANY_REQUESTS);
	}

	@Then("the operator finds the request served")
	public void theOperatorFindsTheRequestServed() {
		assertThat(answer.status()).isEqualTo(SERVED);
	}

	private void turnAway(Map<String, String> headers) {
		startOffTheClocksGrid();
		for (int request = 0; request < ALLOWED_REQUESTS; request++) {
			send(START_PAGE, headers);
			time.passes(BETWEEN_REQUESTS);
		}

		time.passes(LATER_IN_THE_COUNT);
		int beyond = send(START_PAGE, headers).status();

		assertThat(beyond).as("the request beyond the allowed ones").isEqualTo(TOO_MANY_REQUESTS);
		turnedAway = time.now();
	}

	private void startOffTheClocksGrid() {
		service.runAsDeployed();
		time.passes(OFF_THE_GRID);
		firstRequest = time.now();
	}

	private Answer send(String address, Map<String, String> headers) {
		Map<String, String> fromTheClient = new HashMap<>(headers);
		fromTheClient.put(AcceptanceClientIp.HEADER, client);
		return requests.get(address, fromTheClient);
	}

	private static String aFileTheDashboardLoads() {
		try (InputStream index = ClientRequestSteps.class.getResourceAsStream("/static/ui/index.html")) {
			if (index == null) {
				throw new IllegalStateException("The dashboard's start page is not on the classpath");
			}
			Matcher file = DASHBOARD_FILE.matcher(new String(index.readAllBytes(), UTF_8));
			if (!file.find()) {
				throw new IllegalStateException("The dashboard's start page loads no file");
			}
			return file.group(1);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
