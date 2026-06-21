package dev.schoenberg.evergore.protocolParser;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Month;
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

import dev.schoenberg.evergore.protocolParser.businessLogic.banking.BankEntry;
import dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformation;
import dev.schoenberg.evergore.protocolParser.businessLogic.storage.StorageEntry;
import dev.schoenberg.evergore.protocolParser.dataExtraction.EvergoreDataExtractor;
import dev.schoenberg.evergore.protocolParser.dataExtraction.PageSource;
import dev.schoenberg.evergore.protocolParser.dataExtraction.PostCollectionHook;
import dev.schoenberg.evergore.protocolParser.dataExtraction.website.SeleniumPageSource;
import dev.schoenberg.evergore.protocolParser.database.PreDatabaseConnectionHook;
import dev.schoenberg.evergore.protocolParser.database.bank.BankDatabaseRepository;
import dev.schoenberg.evergore.protocolParser.database.metaInformation.MetaInformationDatabaseRepository;
import dev.schoenberg.evergore.protocolParser.database.storage.StorageDatabaseRepository;
import dev.schoenberg.evergore.protocolParser.helper.config.Configuration;

import static dev.schoenberg.evergore.protocolParser.TestHelper.calculateLevenshteinDistance;
import static dev.schoenberg.evergore.protocolParser.businessLogic.Constants.APP_ZONE;
import static dev.schoenberg.evergore.protocolParser.businessLogic.base.TransferType.EINLAGERUNG;
import static dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey.getBankPlacement;
import static dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey.getBankWithdrawl;
import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.silentThrow;
import static dev.schoenberg.evergore.protocolParser.rest.controller.OutputFormatter.NEWLINE;
import static java.time.Duration.ofMinutes;
import static java.time.Instant.EPOCH;
import static java.util.Arrays.asList;
import static kong.unirest.Unirest.config;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@MicronautTest
class SmokeTest {
	private static final String TEST_DATABASE_PATH = "src/test/resources/smokeTest.sqlite";

	static {
		silentThrow(() -> Files.deleteIfExists(Paths.get(TEST_DATABASE_PATH)));
	}

	private @Inject EmbeddedServer server;
	private @Inject Configuration config;
	private @Inject Logger logger;
	private @Inject BootSignalRecorder signals;

	@BeforeEach
	public void setup() {
		config().verifySsl(false);
		config().defaultBaseUrl("http://localhost:" + server.getPort());
	}

	@Test
	void applicationIsStarting() {
		assertTrue(server.isRunning());

		boolean finishedInTime = signals.awaitCollection(ofMinutes(1));

		assertFalse(signals.exceptionOccurred());
		assertTrue(signals.dataLoaded());
		assertTrue(finishedInTime);
	}

	@Test
	void retrieveDataViaBankEndpoint() {
		Instant time = LocalDateTime.of(1900, Month.MAY, 4, 13, 37).atZone(APP_ZONE).toInstant();
		BankDatabaseRepository.get(config, logger, () -> {}).add(asList(new BankEntry(time, "TestAvatar", 42, EINLAGERUNG)));

		HttpResponse<String> response = get("/avatars/TestAvatar/bank");

		assertTrue(response.getStatus() >= 200 && response.getStatus() < 300, "Status code was: " + response.getStatus());
		String content = response.getBody();
		assertClosest(content, "<th>TimeStamp</th><th>Avatar</th><th>Amount</th><th>TransferType</th>");
		assertClosest(content, "<td>04.05.1900 13:37</td><td>TestAvatar</td><td>42</td><td>Einlagerung</td>");
	}

	@Test
	void retrieveDataViaStorageEndpoint() {
		Instant time = LocalDateTime.of(1900, Month.MAY, 4, 13, 37).atZone(APP_ZONE).toInstant();
		StorageDatabaseRepository.get(config, logger, () -> {}).add(asList(new StorageEntry(time, "TestAvatar", 1, "TestItem", 42, EINLAGERUNG)));

		HttpResponse<String> response = get("/avatars/TestAvatar/storage");

		assertTrue(response.getStatus() >= 200 && response.getStatus() < 300, "Status code was: " + response.getStatus());
		String content = response.getBody();

		assertClosest(content, "<th>TimeStamp</th><th>Avatar</th><th>Quantity</th><th>Name</th><th>Quality</th><th>TransferType</th>");
		assertClosest(content, "<td>04.05.1900 13:37</td><td>TestAvatar</td><td>1</td><td>TestItem</td><td>42</td><td>Einlagerung</td>");
	}

	@Test
	void retrieveDataViaOverviewEndpoint() {
		String avatar = "TestAvatar";
		BankDatabaseRepository.get(config, logger, () -> {}).add(asList(new BankEntry(EPOCH, avatar, 0, EINLAGERUNG)));
		MetaInformation<Long> placement = new MetaInformation<>(getBankPlacement(avatar), 1337L);
		MetaInformation<Long> withdrawl = new MetaInformation<>(getBankWithdrawl(avatar), 42L);
		MetaInformationDatabaseRepository.get(config, logger, () -> {}).add(asList(placement, withdrawl));

		HttpResponse<String> response = get("/overview");

		assertTrue(response.getStatus() >= 200 && response.getStatus() < 300, "Status code was: " + response.getStatus());
		String content = response.getBody();
		assertClosest(content, "<th>Avatar</th><th>Entnommen</th><th>Eingelagert</th>");
		assertClosest(content, "<td>TestAvatar</td><td>42</td><td>1337</td>");
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

	private void assertClosest(String content, String expected) {
		if (!content.contains(expected)) {
			List<String> contenLines = asList(content.split(NEWLINE));
			String closest = findClosestString(contenLines, expected);
			assertEquals(expected.trim(), closest.trim());
		}
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
			throwable.printStackTrace();
		}
	}

	@MockBean(Configuration.class)
	Configuration configDto() {
		return new TestConfiguration();
	}

	public static class TestConfiguration extends Configuration {
		@Override
		public String getDatabasePath() {
			return TEST_DATABASE_PATH;
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
		return Unirest.get(endpoint + "?token=secret_token").asString();
	}

	private String findClosestString(List<String> strings, String target) {
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
