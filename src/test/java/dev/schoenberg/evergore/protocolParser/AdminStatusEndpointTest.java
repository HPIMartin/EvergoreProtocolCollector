package dev.schoenberg.evergore.protocolParser;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.scheduling.DefaultTaskExceptionHandler;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.schoenberg.evergore.protocolParser.application.EvergoreDataExtractor;
import dev.schoenberg.evergore.protocolParser.application.LastRunStatus;
import dev.schoenberg.evergore.protocolParser.dataExtraction.PostCollectionHook;
import dev.schoenberg.evergore.protocolParser.database.PreDatabaseConnectionHook;
import dev.schoenberg.evergore.protocolParser.helper.config.Configuration;

import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.silentThrow;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

@MicronautTest
class AdminStatusEndpointTest {

	private static final Path WORKING_DB = Paths.get("build/tmp/adminStatus/adminStatus.sqlite");

	static {
		silentThrow(() -> {
			Files.createDirectories(WORKING_DB.getParent());
			Files.deleteIfExists(WORKING_DB);
			try (InputStream src = AdminStatusEndpointTest.class.getResourceAsStream("/testdata.sqlite")) {
				Files.copy(src, WORKING_DB);
			}
		});
	}

	private @Inject EmbeddedServer server;
	private @Inject BootSignalRecorder signals;
	private @Inject LastRunStatus lastRunStatus;

	@BeforeEach
	void setup() {
		Unirest.config().verifySsl(false);
		Unirest.config().defaultBaseUrl("http://localhost:" + server.getPort());
		signals.awaitCollection();
	}

	@Test
	void adminStatusEndpointIsAccessibleWithoutToken() {
		HttpResponse<String> response = Unirest.get("/api/v1/admin/status").asString();

		assertThat(response.getStatus()).isEqualTo(200);
	}

	@Test
	void reportsTheStampAndBothSuccessfulOutcomesOfTheRunThatJustHappened() {
		JSONObject body = adminStatus();

		assertThat(presenceOf(body, "lastUpdated", "lastSuccessfulScrape", "lastSuccessfulRecompute"))
				.containsOnly(entry("lastUpdated", true), entry("lastSuccessfulScrape", true), entry("lastSuccessfulRecompute", true));
	}

	@Test
	void reportsAFailedScrapeAndAFailedRecomputeAsTheirOwnInstants() {
		lastRunStatus.recordScrapeFailure(Instant.parse("2026-09-06T03:00:00Z"));
		lastRunStatus.recordRecomputeFailure(Instant.parse("2026-09-06T03:00:05Z"));

		JSONObject body = adminStatus();

		assertThat(Map.of("lastScrapeFailure", body.getString("lastScrapeFailure"), "lastRecomputeFailure", body.getString("lastRecomputeFailure")))
				.containsOnly(entry("lastScrapeFailure", "2026-09-06T03:00:00Z"), entry("lastRecomputeFailure", "2026-09-06T03:00:05Z"));
	}

	@Test
	void namesEveryUnknownItemAndFailedAvatarOfTheLastRunOnceAndInOrder() {
		lastRunStatus.recordSuccessfulRecompute(Instant.parse("2026-09-06T03:00:10Z"), List.of("Unobtainium", "Unobtainium"), List.of("Zwerg", "Alrik"));

		JSONObject body = adminStatus();

		assertThat(Map.of("unknownItemNames", toList(body.getJSONArray("unknownItemNames")), "failedAvatarNames", toList(body.getJSONArray("failedAvatarNames"))))
				.containsOnly(entry("unknownItemNames", List.of("Unobtainium")), entry("failedAvatarNames", List.of("Alrik", "Zwerg")));
	}

	private static JSONObject adminStatus() {
		return new JSONObject(Unirest.get("/api/v1/admin/status").asString().getBody());
	}

	private static Map<String, Boolean> presenceOf(JSONObject body, String... fields) {
		Map<String, Boolean> present = new HashMap<>();
		for (String field : fields) {
			present.put(field, !body.isNull(field));
		}
		return present;
	}

	private static List<String> toList(JSONArray array) {
		List<String> values = new ArrayList<>();
		for (int index = 0; index < array.length(); index++) {
			values.add(array.getString(index));
		}
		return values;
	}

	@MockBean(Configuration.class)
	Configuration configurationMock() {
		return new TestConfiguration();
	}

	public static class TestConfiguration extends Configuration {
		@Override
		public String getDatabasePath() {
			return WORKING_DB.toString();
		}

		@Override
		public int getCollectorInitialDelaySeconds() {
			return 0;
		}
	}

	@MockBean(EvergoreDataExtractor.class)
	TestEvergoreDataExtractor testEvergoreDataExtractor() {
		return new TestEvergoreDataExtractor();
	}

	public static class TestEvergoreDataExtractor extends EvergoreDataExtractor {
		public TestEvergoreDataExtractor() {
			super(null, null, null, null);
		}

		@Override
		public void loadData() {}
	}

	@MockBean(PreDatabaseConnectionHook.class)
	PreDatabaseConnectionHook databaseHook() {
		return () -> {};
	}

	@MockBean(PostCollectionHook.class)
	PostCollectionHook collectionHook(BootSignalRecorder recorder) {
		return recorder::recordCollectionFinished;
	}

	@MockBean(DefaultTaskExceptionHandler.class)
	DefaultTaskExceptionHandler exceptionHandler(BootSignalRecorder recorder) {
		return new TestTaskExceptionHandler(recorder);
	}

	public static class TestTaskExceptionHandler extends DefaultTaskExceptionHandler {
		private final BootSignalRecorder signals;

		public TestTaskExceptionHandler(BootSignalRecorder signals) {
			this.signals = signals;
		}

		@Override
		public void handle(Object bean, Throwable throwable) {
			signals.recordException();
		}
	}
}
