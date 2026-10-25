package dev.schoenberg.evergore.protocolParser.acceptance.operator;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import dev.schoenberg.evergore.protocolParser.acceptance.service.RunningService;
import dev.schoenberg.evergore.protocolParser.acceptance.world.ScenarioTime;
import dev.schoenberg.evergore.protocolParser.rest.filter.AcceptanceClientIp;

public class ClientTraffic {
	public static final String LOGIN_PAGE = "/login";
	public static final String OVERVIEWS_FIGURES = "/api/v1/avatars";
	public static final int SERVED = 200;
	public static final int TOO_MANY_REQUESTS = 429;

	private static final String A_CLIENT = "198.51.100.1";
	private static final Duration OFF_THE_GRID = Duration.ofMillis(7_250);

	private final ServiceRequests requests;
	private final RunningService service;
	private final ScenarioTime time;
	private final ClientAnswer received;
	private String client = A_CLIENT;
	private Optional<String> login = Optional.empty();
	private Instant firstRequest;
	private Instant turnedAway;
	private List<Integer> statuses = List.of();

	public ClientTraffic(ServiceRequests requests, RunningService service, ScenarioTime time, ClientAnswer received) {
		this.requests = requests;
		this.service = service;
		this.time = time;
		this.received = received;
	}

	public void startOffTheClocksGrid() {
		service.runAsDeployed();
		time.passes(OFF_THE_GRID);
		firstRequest = time.now();
	}

	public void useClient(String address, Optional<String> loginValue) {
		client = address;
		login = loginValue;
	}

	public String client() {
		return client;
	}

	public String addressWithTheClientsRights() {
		return login.isPresent() ? OVERVIEWS_FIGURES : LOGIN_PAGE;
	}

	public Map<String, String> withTheClientsLogin(Map<String, String> headers) {
		return login.map(value -> LoginRequests.withTheLogin(headers, value)).orElse(headers);
	}

	public void setTimeAfterTheFirstRequest(Duration delay) {
		time.setTo(firstRequest.plus(delay));
	}

	public void setTimeAfterTheClientWasTurnedAway(Duration delay) {
		time.setTo(turnedAway.plus(delay));
	}

	public void rememberTheClientWasTurnedAwayNow() {
		turnedAway = time.now();
	}

	public Instant firstRequest() {
		return firstRequest;
	}

	public List<Integer> statuses() {
		return statuses;
	}

	public void rememberStatuses(List<Integer> answered) {
		statuses = answered;
	}

	public List<Integer> send(int count, String address, Map<String, String> headers) {
		List<Integer> answered = new ArrayList<>();
		for (int request = 0; request < count; request++) {
			Answer answer = sendOne(address, headers);
			received.remember(answer);
			answered.add(answer.status());
		}
		statuses = answered;
		return answered;
	}

	private Answer sendOne(String address, Map<String, String> headers) {
		Map<String, String> fromTheClient = new HashMap<>(headers);
		fromTheClient.put(AcceptanceClientIp.HEADER, client);
		return requests.get(address, fromTheClient);
	}
}
