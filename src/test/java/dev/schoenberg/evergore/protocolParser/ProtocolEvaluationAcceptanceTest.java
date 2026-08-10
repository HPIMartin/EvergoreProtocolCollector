package dev.schoenberg.evergore.protocolParser;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;

import jakarta.inject.Inject;

import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.scheduling.DefaultTaskExceptionHandler;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import dev.schoenberg.evergore.protocolParser.application.EvergoreDataExtractor;
import dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationRepository;
import dev.schoenberg.evergore.protocolParser.dataExtraction.PostCollectionHook;
import dev.schoenberg.evergore.protocolParser.database.PreDatabaseConnectionHook;
import dev.schoenberg.evergore.protocolParser.helper.config.Configuration;

import static dev.schoenberg.evergore.protocolParser.businessLogic.Constants.APP_ZONE;
import static dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey.getLastUpdatedKey;
import static dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey.getStoragePlacement;
import static dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey.getStorageWithdrawl;
import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.silentThrow;
import static io.micronaut.http.HttpStatus.BAD_REQUEST;
import static io.micronaut.http.HttpStatus.NOT_FOUND;
import static io.micronaut.http.HttpStatus.OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.within;

@MicronautTest
class ProtocolEvaluationAcceptanceTest {
	private static final Path WORKING_DB = Paths.get("build/tmp/acceptance/protocolEvaluation.sqlite");

	static {
		silentThrow(() -> {
			Files.createDirectories(WORKING_DB.getParent());
			Files.deleteIfExists(WORKING_DB);
			try (InputStream src = ProtocolEvaluationAcceptanceTest.class.getResourceAsStream("/testdata.sqlite")) {
				Files.copy(src, WORKING_DB);
			}
		});
	}

	private @Inject EmbeddedServer server;
	private @Inject BootSignalRecorder signals;

	@BeforeEach
	void setup() {
		Unirest.config().verifySsl(false);
		Unirest.config().defaultBaseUrl("http://localhost:" + server.getPort());
		signals.awaitCollection();
	}

	@Test
	void avatarSummariesCarryTheBankTotalsOfEveryAvatarOfBothLedgers() {
		HttpResponse<String> response = get("/api/v1/avatars");

		assertThat(response.getStatus()).isEqualTo(OK.getCode());
		JSONObject body = new JSONObject(response.getBody());
		assertThat(body.getInt("page")).isZero();
		assertThat(body.getInt("size")).isEqualTo(100);
		assertThat(body.getLong("totalCount")).isEqualTo(4);
		assertThat(body.getJSONArray("items").toString())
				.isEqualTo("[{\"avatar\":\"Aurora\",\"withdrawn\":200,\"deposited\":1500}," + "{\"avatar\":\"Boreas\",\"withdrawn\":0,\"deposited\":750},"
						+ "{\"avatar\":\"Brynja\",\"withdrawn\":0,\"deposited\":0}," + "{\"avatar\":\"Calix\",\"withdrawn\":300,\"deposited\":0}]");
	}

	@Test
	void avatarSummariesCarryTheLastUpdatedInstantOfTheCompletedRun() {
		String lastUpdated = lastUpdatedOfSummaries();

		assertThatCode(() -> Instant.parse(lastUpdated)).as("lastUpdated must be an ISO-8601 instant, was " + lastUpdated).doesNotThrowAnyException();
	}

	@Test
	void lastUpdatedIsSerializedInTheUtcFormRatherThanAnOffsetForm() {
		String lastUpdated = lastUpdatedOfSummaries();

		assertThat(lastUpdated).endsWith("Z");
	}

	@Test
	void lastUpdatedReadBackInTheApplicationZoneIsTheStoredWallClockTime() {
		String lastUpdated = lastUpdatedOfSummaries();

		LocalDateTime inApplicationZone = Instant.parse(lastUpdated).atZone(APP_ZONE).toLocalDateTime();

		assertThat(inApplicationZone).isEqualTo(storedLastUpdated());
	}

	@Test
	void aurorasBankPageCarriesEveryStoredEntryNewestFirst() {
		HttpResponse<String> response = get("/api/v1/avatars/Aurora/bank");

		assertThat(response.getStatus()).isEqualTo(OK.getCode());
		assertThat(response.getBody())
				.isEqualTo("{\"page\":0,\"size\":100,\"totalCount\":3,\"items\":["
						+ "{\"timestamp\":\"2024-01-12T11:00:00Z\",\"avatar\":\"Aurora\",\"amount\":200,\"transferType\":\"WITHDRAWAL\"},"
						+ "{\"timestamp\":\"2024-01-11T10:00:00Z\",\"avatar\":\"Aurora\",\"amount\":500,\"transferType\":\"DEPOSIT\"},"
						+ "{\"timestamp\":\"2024-01-10T09:00:00Z\",\"avatar\":\"Aurora\",\"amount\":1000,\"transferType\":\"DEPOSIT\"}]}");
	}

	@Test
	void aurorasStoragePageCarriesEveryStoredEntryNewestFirst() {
		HttpResponse<String> response = get("/api/v1/avatars/Aurora/storage");

		assertThat(response.getStatus()).isEqualTo(OK.getCode());
		assertThat(response.getBody())
				.isEqualTo("{\"page\":0,\"size\":100,\"totalCount\":3,\"items\":["
						+ "{\"timestamp\":\"2024-01-17T11:00:00Z\",\"avatar\":\"Aurora\",\"quantity\":1,\"name\":\"Kristall\",\"quality\":100,\"transferType\":\"WITHDRAWAL\"},"
						+ "{\"timestamp\":\"2024-01-16T10:00:00Z\",\"avatar\":\"Aurora\",\"quantity\":2,\"name\":\"Magische Ätherbinde\",\"quality\":100,\"transferType\":\"DEPOSIT\"},"
						+ "{\"timestamp\":\"2024-01-15T09:00:00Z\",\"avatar\":\"Aurora\",\"quantity\":10,\"name\":\"Kupfererz\",\"quality\":100,\"transferType\":\"DEPOSIT\"}]}");
	}

	@Test
	void aSecondSummaryPageCarriesTheRemainingAvatarsAndTheTotalCount() {
		HttpResponse<String> response = get("/api/v1/avatars?page=1&size=2");

		assertThat(response.getStatus()).isEqualTo(OK.getCode());
		assertThat(response.getBody()).contains("\"page\":1", "\"size\":2", "\"totalCount\":4", "\"items\":[{\"avatar\":\"Brynja\"");
	}

	@Test
	void aBankPageBeyondTheLastEntryIsEmptyButStillReportsTheTotalCount() {
		HttpResponse<String> response = get("/api/v1/avatars/Aurora/bank?page=9&size=2");

		assertThat(response.getStatus()).isEqualTo(OK.getCode());
		assertThat(response.getBody()).isEqualTo("{\"page\":9,\"size\":2,\"totalCount\":3,\"items\":[]}");
	}

	@Test
	void aKnownAvatarWithoutStorageRowsGetsAnEmptyPageRatherThanA404() {
		HttpResponse<String> response = get("/api/v1/avatars/Calix/storage");

		assertThat(response.getStatus()).isEqualTo(OK.getCode());
		assertThat(response.getBody()).isEqualTo("{\"page\":0,\"size\":100,\"totalCount\":0,\"items\":[]}");
	}

	@Test
	void anAvatarWithoutBankRowsGetsAnEmptyPageRatherThanA404() {
		HttpResponse<String> response = get("/api/v1/avatars/Brynja/bank");

		assertThat(response.getStatus()).isEqualTo(OK.getCode());
		assertThat(response.getBody()).isEqualTo("{\"page\":0,\"size\":100,\"totalCount\":0,\"items\":[]}");
	}

	@Test
	void anUnknownAvatarHasNoBankResource() {
		int status = statusOfGet("/api/v1/avatars/Nobody/bank");

		assertThat(status).isEqualTo(NOT_FOUND.getCode());
	}

	@Test
	void anUnknownAvatarHasNoStorageResource() {
		int status = statusOfGet("/api/v1/avatars/Nobody/storage");

		assertThat(status).isEqualTo(NOT_FOUND.getCode());
	}

	@ParameterizedTest
	@ValueSource(strings = {"/api/v1/avatars?page=-1", "/api/v1/avatars?size=0", "/api/v1/avatars?size=1001", "/api/v1/avatars/Aurora/bank?page=-1",
			"/api/v1/avatars/Aurora/bank?size=0", "/api/v1/avatars/Aurora/bank?size=1001", "/api/v1/avatars/Aurora/storage?page=-1", "/api/v1/avatars/Aurora/storage?size=0",
			"/api/v1/avatars/Aurora/storage?size=1001"})
	void rejectsAPagingWindowOutsideTheAllowedBounds(String endpoint) {
		int status = statusOfGet(endpoint);

		assertThat(status).isEqualTo(BAD_REQUEST.getCode());
	}

	@Test
	void storageValuationIsCorrectAtBeanLevel() {
		MetaInformationRepository metaRepo = server.getApplicationContext().getBean(MetaInformationRepository.class);

		assertThat(metaRepo.<Double>get(getStoragePlacement("Aurora"))).isPresent().hasValueSatisfying(v -> assertThat(v).isCloseTo(185.04, within(1e-6)));

		assertThat(metaRepo.<Double>get(getStorageWithdrawl("Aurora"))).isPresent().hasValueSatisfying(v -> assertThat(v).isCloseTo(300.0, within(1e-6)));

		assertThat(metaRepo.<Double>get(getStoragePlacement("Boreas"))).isPresent().hasValueSatisfying(v -> assertThat(v).isCloseTo(46.26, within(1e-6)));
	}

	@Test
	void unknownEndpointReturns4xx() {
		int statusCode = statusOfGet("/thatEndpointDoesNotExist");

		assertThat(statusCode).isBetween(400, 499);
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

	private String lastUpdatedOfSummaries() {
		return new JSONObject(get("/api/v1/avatars").getBody()).getString("lastUpdated");
	}

	private LocalDateTime storedLastUpdated() {
		return server.getApplicationContext().getBean(MetaInformationRepository.class).get(getLastUpdatedKey()).orElseThrow();
	}

	private static int statusOfGet(String endpoint) {
		return get(endpoint).getStatus();
	}

	private static HttpResponse<String> get(String endpoint) {
		String separator = endpoint.contains("?") ? "&" : "?";
		return Unirest.get(endpoint + separator + "token=test-token").asString();
	}
}
