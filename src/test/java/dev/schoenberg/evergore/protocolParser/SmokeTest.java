package dev.schoenberg.evergore.protocolParser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;

import jakarta.inject.Inject;

import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.scheduling.DefaultTaskExceptionHandler;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.schoenberg.evergore.protocolParser.application.EvergoreDataExtractor;
import dev.schoenberg.evergore.protocolParser.businessLogic.banking.BankEntry;
import dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformation;
import dev.schoenberg.evergore.protocolParser.businessLogic.storage.StorageEntry;
import dev.schoenberg.evergore.protocolParser.dataExtraction.PageSource;
import dev.schoenberg.evergore.protocolParser.dataExtraction.PostCollectionHook;
import dev.schoenberg.evergore.protocolParser.dataExtraction.website.SeleniumPageSource;
import dev.schoenberg.evergore.protocolParser.database.PreDatabaseConnectionHook;
import dev.schoenberg.evergore.protocolParser.database.bank.BankDatabaseRepository;
import dev.schoenberg.evergore.protocolParser.database.metaInformation.MetaInformationDatabaseRepository;
import dev.schoenberg.evergore.protocolParser.database.storage.StorageDatabaseRepository;
import dev.schoenberg.evergore.protocolParser.helper.config.Configuration;

import static dev.schoenberg.evergore.protocolParser.businessLogic.base.TransferType.EINLAGERUNG;
import static dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey.getBankPlacement;
import static dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey.getBankWithdrawl;
import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.silentThrow;
import static java.time.Instant.EPOCH;
import static java.util.Arrays.asList;
import static kong.unirest.Unirest.config;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@MicronautTest
class SmokeTest {
	private static final Path TEST_DATABASE_PATH = Paths.get("build", "tmp", "smokeTest.sqlite");
	private static final Instant SEEDED_TIME = Instant.parse("1900-05-04T12:37:00Z");

	private @Inject EmbeddedServer server;
	private @Inject Configuration config;
	private @Inject Logger logger;
	private @Inject BootSignalRecorder signals;

	@BeforeEach
	public void setup() {
		config().verifySsl(false);
		config().defaultBaseUrl("http://localhost:" + server.getPort());
		signals.awaitCollection();
	}

	@Test
	void applicationIsStarting() {
		assertTrue(server.isRunning());

		signals.awaitCollection();

		assertFalse(signals.exceptionOccurred());
		assertTrue(signals.dataLoaded());
	}

	@Test
	void retrieveDataViaBankEndpoint() {
		BankDatabaseRepository.get(config, logger, () -> {}).add(asList(new BankEntry(SEEDED_TIME, "BankTestAvatar", 42, EINLAGERUNG)));

		HttpResponse<String> response = get("/api/v1/avatars/BankTestAvatar/bank");

		assertTrue(response.getStatus() >= 200 && response.getStatus() < 300, "Status code was: " + response.getStatus());
		assertEquals("{\"page\":0,\"size\":100,\"totalCount\":1,\"items\":["
				+ "{\"timestamp\":\"1900-05-04T12:37:00Z\",\"avatar\":\"BankTestAvatar\",\"amount\":42,\"transferType\":\"DEPOSIT\"}]}", response.getBody());
	}

	@Test
	void retrieveDataViaStorageEndpoint() {
		StorageDatabaseRepository.get(config, logger, () -> {}).add(asList(new StorageEntry(SEEDED_TIME, "StorageTestAvatar", 1, "TestItem", 42, EINLAGERUNG)));

		HttpResponse<String> response = get("/api/v1/avatars/StorageTestAvatar/storage");

		assertTrue(response.getStatus() >= 200 && response.getStatus() < 300, "Status code was: " + response.getStatus());
		assertEquals("{\"page\":0,\"size\":100,\"totalCount\":1,\"items\":["
				+ "{\"timestamp\":\"1900-05-04T12:37:00Z\",\"avatar\":\"StorageTestAvatar\",\"quantity\":1,\"name\":\"TestItem\",\"quality\":42,\"transferType\":\"DEPOSIT\"}]}",
				response.getBody());
	}

	@Test
	void retrieveDataViaOverviewEndpoint() {
		String avatar = "OverviewTestAvatar";
		BankDatabaseRepository.get(config, logger, () -> {}).add(asList(new BankEntry(EPOCH, avatar, 0, EINLAGERUNG)));
		MetaInformation<Long> placement = new MetaInformation<>(getBankPlacement(avatar), 1337L);
		MetaInformation<Long> withdrawl = new MetaInformation<>(getBankWithdrawl(avatar), 42L);
		MetaInformationDatabaseRepository.get(config, logger, () -> {}).add(asList(placement, withdrawl));

		HttpResponse<String> response = get("/api/v1/avatars");

		assertTrue(response.getStatus() >= 200 && response.getStatus() < 300, "Status code was: " + response.getStatus());
		assertTrue(response.getBody().contains("{\"avatar\":\"OverviewTestAvatar\",\"bankWithdrawn\":42,\"bankDeposited\":1337}"), "Body was: " + response.getBody());
	}

	@Test
	void requestNotExisitingEndpoint() {
		int statusCode = get("/thatEndpointDoesNotExist").getStatus();

		assertTrue(statusCode >= 400 && statusCode < 500);
	}

	@Test
	void pageSourceResolvesToTheSeleniumAdapter() {
		PageSource pageSource = server.getApplicationContext().getBean(PageSource.class);

		assertTrue(pageSource instanceof SeleniumPageSource, "Expected SeleniumPageSource but was " + pageSource.getClass());
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

	@MockBean(Configuration.class)
	Configuration configDto() {
		return new TestConfiguration();
	}

	public static class TestConfiguration extends Configuration {
		static {
			silentThrow(() -> {
				Files.createDirectories(TEST_DATABASE_PATH.getParent());
				for (String suffix : List.of("", "-journal", "-wal", "-shm")) {
					Files.deleteIfExists(Paths.get(TEST_DATABASE_PATH + suffix));
				}
			});
		}

		@Override
		public String getDatabasePath() {
			return TEST_DATABASE_PATH.toString();
		}

		@Override
		public int getCollectorInitialDelaySeconds() {
			return 0;
		}
	}

	@MockBean(EvergoreDataExtractor.class)
	TestEvergoreDataExtractor testEvergoreDataExtractor(BootSignalRecorder recorder) {
		return new TestEvergoreDataExtractor(recorder);
	}

	public static class TestEvergoreDataExtractor extends EvergoreDataExtractor {
		private final BootSignalRecorder signals;

		public TestEvergoreDataExtractor(BootSignalRecorder signals) {
			super(null, null, null, null);
			this.signals = signals;
		}

		@Override
		public void loadData() {
			signals.recordDataLoaded();
		}
	}

	private HttpResponse<String> get(String endpoint) {
		return Unirest.get(endpoint + "?token=test-token").asString();
	}
}
