package dev.schoenberg.evergore.protocolParser;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
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
import static java.time.Instant.now;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
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

	private static boolean exceptionFound;
	private static boolean collectionFinished;
	private static boolean dataIsLoaded;

	@BeforeEach
	public void setup() {
		config().verifySsl(false);
		config().defaultBaseUrl("http://localhost:" + server.getPort());
	}

	@Test
	void applicationIsStarting() {
		assertTrue(server.isRunning());

		boolean finishedInTime = awaitCollectionFinished();

		assertFalse(exceptionFound);
		assertTrue(dataIsLoaded);
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

	private boolean awaitCollectionFinished() {
		Duration MAX_WAITING_TIME = ofMinutes(1);
		Instant stopAwaiting = now().plus(MAX_WAITING_TIME);
		while (!collectionFinished) {
			if (now().isAfter(stopAwaiting)) {
				return false;
			}
			silentThrow(() -> MILLISECONDS.sleep(50));
		}
		return true;
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
	PostCollectionHook collectionHook() {
		return this::collectionFinished;
	}

	private void collectionFinished() {
		collectionFinished = true;
	}

	@MockBean(DefaultTaskExceptionHandler.class)
	DefaultTaskExceptionHandler exceptionHandler() {
		return new TestTaskExceptionHandler();
	}

	public static class TestTaskExceptionHandler extends DefaultTaskExceptionHandler {
		@Override
		public void handle(Object bean, Throwable throwable) {
			exceptionFound = true;
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
	TestEvergoreDataExtractor testEvergoreDataExtractor() {
		return new TestEvergoreDataExtractor();
	}

	public static class TestEvergoreDataExtractor extends EvergoreDataExtractor {
		public TestEvergoreDataExtractor() {
			super(null, null, null, null);
		}

		@Override
		public void loadData() {
			dataIsLoaded = true;
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
