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
	void avatarSummariesCarryAllFourLedgerSumsAndTheNetOfEveryAvatar() {
		HttpResponse<String> response = get("/api/v1/avatars");

		assertThat(response.getStatus()).isEqualTo(OK.getCode());
		JSONObject body = new JSONObject(response.getBody());
		assertThat(body.getInt("page")).isZero();
		assertThat(body.getInt("size")).isEqualTo(100);
		assertThat(body.getLong("totalCount")).isEqualTo(4);
		assertThat(body.getJSONArray("items").toString())
				.isEqualTo(
						"[{\"avatar\":\"Aurora\",\"bankWithdrawn\":200,\"bankDeposited\":1500,\"storageWithdrawn\":300,\"storageDeposited\":308,\"net\":1308,\"donation\":120,\"craftSubsidy\":0,"
								+ "\"lastBankActivity\":\"2024-01-12T11:00:00Z\",\"lastStorageActivity\":\"2024-01-17T11:00:00Z\",\"staleSumsFrom\":null},"
								+ "{\"avatar\":\"Boreas\",\"bankWithdrawn\":0,\"bankDeposited\":750,\"storageWithdrawn\":0,\"storageDeposited\":77,\"net\":827,\"donation\":0,\"craftSubsidy\":0,"
								+ "\"lastBankActivity\":\"2024-02-01T08:00:00Z\",\"lastStorageActivity\":\"2024-02-05T08:00:00Z\",\"staleSumsFrom\":null},"
								+ "{\"avatar\":\"Brynja\",\"bankWithdrawn\":0,\"bankDeposited\":0,\"storageWithdrawn\":0,\"storageDeposited\":1217,\"net\":1217,\"donation\":0,\"craftSubsidy\":240,"
								+ "\"lastBankActivity\":null,\"lastStorageActivity\":\"2024-02-06T09:00:00Z\",\"staleSumsFrom\":null},"
								+ "{\"avatar\":\"Calix\",\"bankWithdrawn\":300,\"bankDeposited\":0,\"storageWithdrawn\":0,\"storageDeposited\":0,\"net\":-300,\"donation\":0,\"craftSubsidy\":0,"
								+ "\"lastBankActivity\":\"2024-03-01T07:00:00Z\",\"lastStorageActivity\":null,\"staleSumsFrom\":null}]");
	}

	@Test
	void avatarSummariesCarryTheLastUpdatedInstantOfTheCompletedRun() {
		String lastUpdated = lastUpdatedOfAdminStatus();

		assertThatCode(() -> Instant.parse(lastUpdated)).as("lastUpdated must be an ISO-8601 instant, was " + lastUpdated).doesNotThrowAnyException();
	}

	@Test
	void lastUpdatedIsSerializedInTheUtcFormRatherThanAnOffsetForm() {
		String lastUpdated = lastUpdatedOfAdminStatus();

		assertThat(lastUpdated).endsWith("Z");
	}

	@Test
	void lastUpdatedReadBackInTheApplicationZoneIsTheStoredWallClockTime() {
		String lastUpdated = lastUpdatedOfAdminStatus();

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
	void theSummariesCarryTheGuildWideTotalsOverEveryAvatarOfBothLedgers() {
		JSONObject totals = new JSONObject(get("/api/v1/avatars").getBody()).getJSONObject("totals");

		assertThat(totals.getLong("bankDeposited")).isEqualTo(2250);
		assertThat(totals.getLong("bankWithdrawn")).isEqualTo(500);
		assertThat(totals.getLong("storageDeposited")).isEqualTo(1602);
		assertThat(totals.getLong("storageWithdrawn")).isEqualTo(300);
		assertThat(totals.getLong("net")).isEqualTo(3052);
		assertThat(totals.getLong("donation")).isEqualTo(120);
		assertThat(totals.getLong("craftSubsidy")).isEqualTo(240);
	}

	@Test
	void theGuildWideTotalsStayTheSameOnAPageThatShowsTwoAvatars() {
		JSONObject totals = new JSONObject(get("/api/v1/avatars?page=1&size=2").getBody()).getJSONObject("totals");

		assertThat(totals.getLong("net")).isEqualTo(3052);
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

		assertThat(metaRepo.snapshot().get(getStoragePlacement("Aurora"))).isPresent().hasValueSatisfying(v -> assertThat(v).isCloseTo(308.4, within(1e-6)));

		assertThat(metaRepo.snapshot().get(getStorageWithdrawl("Aurora"))).isPresent().hasValueSatisfying(v -> assertThat(v).isCloseTo(300.0, within(1e-6)));

		assertThat(metaRepo.snapshot().get(getStoragePlacement("Boreas"))).isPresent().hasValueSatisfying(v -> assertThat(v).isCloseTo(77.1, within(1e-6)));

		assertThat(metaRepo.snapshot().get(getStoragePlacement("Brynja"))).isPresent().hasValueSatisfying(v -> assertThat(v).isCloseTo(1216.8, within(1e-6)));
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

	private String lastUpdatedOfAdminStatus() {
		return new JSONObject(get("/api/v1/admin/status").getBody()).getString("lastUpdated");
	}

	private LocalDateTime storedLastUpdated() {
		return server.getApplicationContext().getBean(MetaInformationRepository.class).snapshot().get(getLastUpdatedKey()).orElseThrow();
	}

	private static int statusOfGet(String endpoint) {
		return get(endpoint).getStatus();
	}

	private static HttpResponse<String> get(String endpoint) {
		String separator = endpoint.contains("?") ? "&" : "?";
		return Unirest.get(endpoint + separator + "token=test-token").asString();
	}
}
