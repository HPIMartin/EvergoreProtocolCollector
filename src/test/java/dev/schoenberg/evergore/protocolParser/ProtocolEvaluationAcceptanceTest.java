package dev.schoenberg.evergore.protocolParser;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
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

import dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationRepository;
import dev.schoenberg.evergore.protocolParser.dataExtraction.EvergoreDataCollectorJob;
import dev.schoenberg.evergore.protocolParser.dataExtraction.EvergoreDataExtractor;
import dev.schoenberg.evergore.protocolParser.dataExtraction.PostCollectionHook;
import dev.schoenberg.evergore.protocolParser.database.PreDatabaseConnectionHook;
import dev.schoenberg.evergore.protocolParser.helper.config.Configuration;

import static dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey.getStoragePlacement;
import static dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey.getStorageWithdrawl;
import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.silentThrow;
import static java.time.Duration.ofMinutes;
import static java.time.Instant.now;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@MicronautTest
class ProtocolEvaluationAcceptanceTest {
	private static final Path WORKING_DB = Paths.get("build/tmp/acceptance/protocolEvaluation.sqlite");

	private static volatile boolean collectionFinished;
	private static volatile boolean exceptionFound;

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

	@BeforeEach
	void setup() {
		Unirest.config().verifySsl(false);
		Unirest.config().defaultBaseUrl("http://localhost:" + server.getPort());
		exceptionFound = false;
		collectionFinished = false;

		EvergoreDataCollectorJob.DELAY_IN_SEC = 0;
	}

	@Test
	void overviewShowsCorrectBankTotalsForAllThreeAvatars() {
		awaitCollectionFinished();

		HttpResponse<String> response = get("/overview");

		assertThat(response.getStatus()).isBetween(200, 299);
		RenderedTable table = RenderedTable.parse(response.getBody());
		assertThat(table.header()).containsExactly("Avatar", "Entnommen", "Eingelagert");

		List<String> aurora = table.rowForFirstCell("Aurora");
		assertThat(aurora).containsExactly("Aurora", "200", "1500");

		List<String> boreas = table.rowForFirstCell("Boreas");
		assertThat(boreas).containsExactly("Boreas", "0", "750");

		List<String> calix = table.rowForFirstCell("Calix");
		assertThat(calix).containsExactly("Calix", "300", "0");
	}

	@Test
	void aurorasBankEndpointShowsExpectedEntry() {
		awaitCollectionFinished();

		HttpResponse<String> response = get("/avatars/Aurora/bank");

		assertThat(response.getStatus()).isBetween(200, 299);
		RenderedTable table = RenderedTable.parse(response.getBody());
		assertThat(table.header()).containsExactly("TimeStamp", "Avatar", "Amount", "TransferType");

		boolean hasPlacement1000 = table.rows().stream().anyMatch(r -> r.size() >= 4 && "Aurora".equals(r.get(1)) && "1000".equals(r.get(2)) && "Einlagerung".equals(r.get(3)));
		assertThat(hasPlacement1000).as("Expected an Aurora EINLAGERUNG 1000 bank row").isTrue();
	}

	@Test
	void aurorasStorageEndpointShowsExpectedEntry() {
		awaitCollectionFinished();

		HttpResponse<String> response = get("/avatars/Aurora/storage");

		assertThat(response.getStatus()).isBetween(200, 299);
		RenderedTable table = RenderedTable.parse(response.getBody());
		assertThat(table.header()).containsExactly("TimeStamp", "Avatar", "Quantity", "Name", "Quality", "TransferType");

		boolean hasMagischeAetherbinde = table
				.rows()
				.stream()
				.anyMatch(r -> r.size() >= 4 && "Aurora".equals(r.get(1)) && "2".equals(r.get(2)) && "Magische Ätherbinde".equals(r.get(3)));
		assertThat(hasMagischeAetherbinde).as("Expected an Aurora Magische Ätherbinde qty 2 storage row").isTrue();
	}

	@Test
	void storageValuationIsCorrectAtBeanLevel() {
		awaitCollectionFinished();

		MetaInformationRepository metaRepo = server.getApplicationContext().getBean(MetaInformationRepository.class);

		assertThat(metaRepo.<Double>get(getStoragePlacement("Aurora"))).isPresent().hasValueSatisfying(v -> assertThat(v).isCloseTo(185.04, within(1e-6)));

		assertThat(metaRepo.<Double>get(getStorageWithdrawl("Aurora"))).isPresent().hasValueSatisfying(v -> assertThat(v).isCloseTo(300.0, within(1e-6)));

		assertThat(metaRepo.<Double>get(getStoragePlacement("Boreas"))).isPresent().hasValueSatisfying(v -> assertThat(v).isCloseTo(46.26, within(1e-6)));
	}

	@Test
	void unknownEndpointReturns4xx() {
		int statusCode = get("/thatEndpointDoesNotExist").getStatus();

		assertThat(statusCode).isBetween(400, 499);
	}

	private void awaitCollectionFinished() {
		Duration maxWait = ofMinutes(1);
		Instant stop = now().plus(maxWait);
		while (!collectionFinished) {
			if (now().isAfter(stop)) {
				return;
			}
			silentThrow(() -> MILLISECONDS.sleep(50));
		}
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
		return new NoOpPreDatabaseConnectionHook();
	}

	public static class NoOpPreDatabaseConnectionHook implements PreDatabaseConnectionHook {
		@Override
		public void run() {}
	}

	@MockBean(PostCollectionHook.class)
	PostCollectionHook collectionHook() {
		return new CollectionFinishedHook();
	}

	public static class CollectionFinishedHook implements PostCollectionHook {
		@Override
		public void run() {
			collectionFinished = true;
		}
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

	private HttpResponse<String> get(String endpoint) {
		return Unirest.get(endpoint + "?token=secret_token").asString();
	}
}
