package dev.schoenberg.evergore.protocolParser;

import static dev.schoenberg.evergore.protocolParser.TestHelper.*;
import static dev.schoenberg.evergore.protocolParser.businessLogic.Constants.*;
import static dev.schoenberg.evergore.protocolParser.businessLogic.base.TransferType.*;
import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.*;
import static dev.schoenberg.evergore.protocolParser.rest.controller.OutputFormatter.*;
import static java.nio.file.Files.*;
import static java.util.Arrays.*;
import static kong.unirest.Unirest.*;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.*;
import java.time.*;
import java.util.*;

import org.junit.jupiter.api.*;

import dev.schoenberg.evergore.protocolParser.businessLogic.banking.*;
import dev.schoenberg.evergore.protocolParser.businessLogic.storage.*;
import dev.schoenberg.evergore.protocolParser.database.bank.*;
import dev.schoenberg.evergore.protocolParser.database.storage.*;
import dev.schoenberg.evergore.protocolParser.helper.config.*;
import io.micronaut.runtime.server.*;
import io.micronaut.test.annotation.*;
import io.micronaut.test.extensions.junit5.annotation.*;
import jakarta.inject.*;
import kong.unirest.*;

@MicronautTest
public class SmokeTest {
	private static final String TEST_DATABASE_PATH = "src/test/resources/smokeTest.sqlite";

	private @Inject EmbeddedServer server;
	private @Inject Configuration config;
	private @Inject Logger logger;

	@BeforeEach
	public void setup() {
		config().verifySsl(false);
		config().defaultBaseUrl("http://localhost:" + server.getPort());
	}

	@BeforeAll
	public static void deleteTestDatabase() {
		silentThrow(() -> deleteIfExists(Paths.get(TEST_DATABASE_PATH)));
	}

	public static class TestConfiguration extends Configuration {
		@Override
		public String getDatabasePath() {
			return "src/test/resources/testdata.sqlite";
		}
	}

	@MockBean(Configuration.class)
	Configuration configDto() {
		return new TestConfiguration();
	}

	@Test
	public void applicationIsStarting() {
		assertTrue(server.isRunning());
	}

	@Test
	public void retrieveDataViaBankEndpoint() {
		Instant time = LocalDateTime.of(1900, Month.MAY, 4, 13, 37).atZone(APP_ZONE).toInstant();
		BankDatabaseRepository.get(config, logger).add(asList(new BankEntry(time, "TestAvatar", 42, Einlagerung)));

		HttpResponse<String> response = get("/avatars/TestAvatar/bank");

		assertTrue(response.getStatus() >= 200 && response.getStatus() < 300, "Status code was: " + response.getStatus());
		String content = response.getBody();
		assertClosest(content, "<th>TimeStamp</th><th>Avatar</th><th>Amount</th><th>TransferType</th>");
		assertClosest(content, "<th>04.05.1900 13:37</th><th>TestAvatar</th><th>42</th><th>Einlagerung</th>");
	}

	@Test
	public void retrieveDataViaStorageEndpoint() {
		Instant time = LocalDateTime.of(1900, Month.MAY, 4, 13, 37).atZone(APP_ZONE).toInstant();
		StorageDatabaseRepository.get(config, logger).add(asList(new StorageEntry(time, "TestAvatar", 1, "TestItem", 42, Einlagerung)));

		HttpResponse<String> response = get("/avatars/TestAvatar/storage");

		assertTrue(response.getStatus() >= 200 && response.getStatus() < 300, "Status code was: " + response.getStatus());
		String content = response.getBody();

		assertClosest(content, "<th>TimeStamp</th><th>Avatar</th><th>Quantity</th><th>Name</th><th>Quality</th><th>TransferType</th>");
		assertClosest(content, "<th>04.05.1900 13:37</th><th>TestAvatar</th><th>1</th><th>TestItem</th><th>42</th><th>Einlagerung</th>");
	}

	private void assertClosest(String content, String expected) {
		if (!content.contains(expected)) {
			List<String> contenLines = asList(content.split(NEWLINE));
			String closest = findClosestString(contenLines, expected);
			assertEquals(expected.trim(), closest.trim());
		}
	}

	@Test
	void requestNotExisitingEndpoint() {
		int statusCode = get("/thatEndpointDoesNotExist").getStatus();

		assertTrue(statusCode >= 400 && statusCode < 500);
	}

	private HttpResponse<String> get(String endpoint) {
		return Unirest.get(endpoint + "?token=secret_token").asString();
	}

	private static String findClosestString(List<String> strings, String target) {
		int minDistance = Integer.MAX_VALUE;
		String closestString = null;

		for (String str : strings) {
			int distance = calculateLevenshteinDistance(str, target);
			if (distance < minDistance) {
				minDistance = distance;
				closestString = str;
			}
		}

		return closestString;
	}
}
