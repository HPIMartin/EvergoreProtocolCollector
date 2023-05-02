package dev.schoenberg.evergore.protocolParser;

import static kong.unirest.Unirest.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;

import dev.schoenberg.evergore.protocolParser.helper.config.*;
import io.micronaut.runtime.server.*;
import io.micronaut.test.extensions.junit5.annotation.*;
import jakarta.inject.*;
import kong.unirest.*;

@MicronautTest
public class SmokeTest {
	private @Inject Configuration config;
	private @Inject EmbeddedServer server;

	@BeforeEach
	public void setup() {
		config().verifySsl(false);
		config().defaultBaseUrl("http://localhost:" + server.getPort());

		config.useInMemory = false;
		config.DATABASE_TEMP_SQLITE = "test.sqlite";
	}

	@Test
	public void applicationIsStarting() {
		assertTrue(server.isRunning());
	}

	@Test
	public void retrieveDataViaBankEndpoint() {
		HttpResponse<String> response = get("/avatars/Alessia/bank");

		assertTrue(response.getStatus() >= 200 && response.getStatus() < 300,
				"Status code was: " + response.getStatus());
		String content = response.getBody();
		assertTrue(
				content.contains("TimeStamp                     Avatar              Amount              TransferType"));
		assertTrue(
				content.contains("2023-02-19T16:17:00Z          Alessia             14131               Einlagerung"));
	}

	@Test
	public void retrieveDataViaStorageEndpoint() {
		HttpResponse<String> response = get("/avatars/Alessia/storage");

		assertTrue(response.getStatus() >= 200 && response.getStatus() < 300,
				"Status code was: " + response.getStatus());
		String content = response.getBody();
		System.out.println(content.split("\n")[0]);
		assertTrue(content.contains(
				"TimeStamp                     Avatar              Quantity            Name                           Quality             TransferType"));
		assertTrue(content.contains(
				"2023-02-12T12:12:00Z          Alessia             8                   Mondstaub                      100                 Einlagerung"));
	}

	@Test
	void requestNotExisitingEndpoint() {
		int statusCode = get("/thatEndpointDoesNotExist").getStatus();

		assertTrue(statusCode >= 400 && statusCode < 500);
	}

	private HttpResponse<String> get(String endpoint) {
		return Unirest.get(endpoint + "?token=secret_token").asString();
	}
}
