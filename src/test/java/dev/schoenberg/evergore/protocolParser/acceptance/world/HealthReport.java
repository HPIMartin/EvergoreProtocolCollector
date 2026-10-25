package dev.schoenberg.evergore.protocolParser.acceptance.world;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;

import dev.schoenberg.evergore.protocolParser.acceptance.operator.LoginRequests;
import dev.schoenberg.evergore.protocolParser.acceptance.service.RunningService;

public class HealthReport {
	private static final String ADMIN = "admin";
	private static final ObjectMapper JSON = new ObjectMapper();
	private static final String HEALTH = "/health";
	private static final int OK = 200;
	private static final int UNAVAILABLE = 503;

	private final RunningService service;
	private final LoginRequests logins;
	private final ServiceLog log;
	private JsonNode report;
	private int httpStatus;

	public HealthReport(RunningService service, LoginRequests logins, ServiceLog log) {
		this.service = service;
		this.logins = logins;
		this.log = log;
	}

	public void ask() {
		String login = LoginRequests.loginValueIn(logins.logIn(ADMIN, log.adminKeyNamed()));
		askFor(HEALTH, Map.of("Cookie", LoginRequests.SESSION_COOKIE + "=" + login));
	}

	public void askWithoutToken() {
		askFor(HEALTH, Map.of());
	}

	public String status() {
		return report().path("status").asText();
	}

	public int httpStatus() {
		report();
		return httpStatus;
	}

	public JsonNode lastRunDetails() {
		return report().path("details").path("lastRun").path("details");
	}

	public List<String> roundTripMessages() {
		return textsAt("roundTrips");
	}

	public List<String> abstentionMessages() {
		return textsAt("roundTripAbstentions");
	}

	private void askFor(String address, Map<String, String> headers) {
		HttpResponse<String> response = Unirest.get("http://localhost:" + service.port() + address).headers(headers).asString();
		if (response.getStatus() != OK && response.getStatus() != UNAVAILABLE) {
			throw new AssertionError("The health check answered " + response.getStatus() + ": " + response.getBody());
		}
		httpStatus = response.getStatus();
		report = readTree(response.getBody());
	}

	private JsonNode report() {
		if (report == null) {
			throw new IllegalStateException("The operator has not asked the service for its health yet");
		}
		return report;
	}

	private List<String> textsAt(String field) {
		List<String> texts = new ArrayList<>();
		lastRunDetails().path(field).forEach(node -> texts.add(node.asText()));
		return texts;
	}

	private static JsonNode readTree(String body) {
		try {
			return JSON.readTree(body);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	public String toString() {
		return String.valueOf(report);
	}
}
