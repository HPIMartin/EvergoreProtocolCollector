package dev.schoenberg.evergore.protocolParser;

import java.io.*;
import java.nio.file.*;

import jakarta.inject.*;

import io.micronaut.runtime.server.*;
import io.micronaut.scheduling.*;
import io.micronaut.test.annotation.*;
import io.micronaut.test.extensions.junit5.annotation.*;
import kong.unirest.*;
import org.junit.jupiter.api.*;

import dev.schoenberg.evergore.protocolParser.dataExtraction.*;
import dev.schoenberg.evergore.protocolParser.database.*;
import dev.schoenberg.evergore.protocolParser.helper.config.*;

import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.*;
import static org.assertj.core.api.Assertions.*;

@MicronautTest
class HealthEndpointTest {

	private static final Path WORKING_DB = Paths.get("build/tmp/healthEndpoint/health.sqlite");

	static {
		silentThrow(() -> {
			Files.createDirectories(WORKING_DB.getParent());
			Files.deleteIfExists(WORKING_DB);
			try (InputStream src = HealthEndpointTest.class.getResourceAsStream("/testdata.sqlite")) {
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
	void healthEndpointIsAccessibleWithoutToken() {
		HttpResponse<String> response = Unirest.get("/health").asString();

		assertThat(response.getStatus()).isEqualTo(200);
	}

	@Test
	void healthEndpointBodyContainsLastRunDetail() {
		HttpResponse<String> response = Unirest.get("/health").asString();

		assertThat(response.getBody()).contains("lastRun");
		assertThat(response.getBody()).contains("lastSuccessfulRun");
	}

	@Test
	void protectedEndpointIsRejectedWithoutToken() {
		int status = Unirest.get("/overview").asString().getStatus();

		assertThat(status).isBetween(400, 499);
	}

	@Test
	void healthPrefixedPathThatIsNotTheHealthEndpointStillRequiresToken() {
		int protectedStatus = Unirest.get("/overview").asString().getStatus();
		int healthzStatus = Unirest.get("/healthz").asString().getStatus();

		assertThat(healthzStatus).isEqualTo(protectedStatus);
	}

	@Test
	void protectedEndpointIsRejectedWithWrongToken() {
		int status = Unirest.get("/overview?token=definitely-the-wrong-token").asString().getStatus();

		assertThat(status).isBetween(400, 499);
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
