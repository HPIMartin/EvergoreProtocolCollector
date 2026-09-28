package dev.schoenberg.evergore.protocolParser.acceptance.world;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;

import dev.schoenberg.evergore.protocolParser.acceptance.service.RunningService;

public class HealthReport {
	private static final ObjectMapper JSON = new ObjectMapper();

	private final RunningService service;
	private JsonNode lastRunDetails;

	public HealthReport(RunningService service) {
		this.service = service;
	}

	public void ask() {
		HttpResponse<String> response = Unirest.get("http://localhost:" + service.port() + "/health").asString();
		lastRunDetails = readTree(response.getBody()).path("details").path("lastRun").path("details");
	}

	public List<String> roundTripMessages() {
		return textsAt("roundTrips");
	}

	public List<String> abstentionMessages() {
		return textsAt("roundTripAbstentions");
	}

	private List<String> textsAt(String field) {
		List<String> texts = new ArrayList<>();
		lastRunDetails.path(field).forEach(node -> texts.add(node.asText()));
		return texts;
	}

	private static JsonNode readTree(String body) {
		try {
			return JSON.readTree(body);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
