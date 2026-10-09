package dev.schoenberg.evergore.protocolParser.rest.filter;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.stream.IntStream;

import jakarta.inject.Inject;

import io.micronaut.context.annotation.Property;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

import dev.schoenberg.evergore.protocolParser.RawHttpClient;
import dev.schoenberg.evergore.protocolParser.application.EvergoreDataExtractor;
import dev.schoenberg.evergore.protocolParser.dataExtraction.PostCollectionHook;
import dev.schoenberg.evergore.protocolParser.database.PreDatabaseConnectionHook;
import dev.schoenberg.evergore.protocolParser.helper.config.Configuration;

import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.silentThrow;
import static io.micronaut.http.HttpStatus.OK;
import static org.assertj.core.api.Assertions.assertThat;

@MicronautTest(environments = "ratelimit", rebuildContext = true)
@Property(name = "micronaut.server.idle-timeout", value = "3s")
class UnreadableHeadIdleTimeoutServerTest {

	private static final Path WORKING_DB = Paths.get("build/tmp/unreadableHeadIdleTimeout/unreadableHeadIdleTimeout.sqlite");
	private static final Duration LESS_THAN_THE_IDLE_TIMEOUT = Duration.ofMillis(2500);
	private static final String OVERSIZED_TARGET = "a".repeat(5000);

	static {
		silentThrow(() -> {
			Files.createDirectories(WORKING_DB.getParent());
			Files.deleteIfExists(WORKING_DB);
			try (InputStream src = UnreadableHeadIdleTimeoutServerTest.class.getResourceAsStream("/testdata.sqlite")) {
				Files.copy(src, WORKING_DB);
			}
		});
	}

	private @Inject EmbeddedServer server;

	@Test
	void neitherAnswersNorCountsAHeadThatNeverEndsAndLeavesItToTheIdleTimeout() {
		RawHttpClient rawClient = new RawHttpClient(server.getPort());
		String headWithoutItsBlankLine = "GET /" + OVERSIZED_TARGET + " HTTP/1.1\r\nHost: localhost\r\nUser-Agent: idle\r\n";
		long startedAt = System.nanoTime();

		String answer = rawClient.answerUntilTheServerCloses(headWithoutItsBlankLine);
		Duration waited = Duration.ofNanos(System.nanoTime() - startedAt);
		List<Integer> statuses = IntStream.range(0, 2).mapToObj(_ -> rawClient.statusOf("/favicon.ico")).toList();

		assertThat(answer).isEmpty();
		assertThat(waited).isGreaterThanOrEqualTo(LESS_THAN_THE_IDLE_TIMEOUT);
		assertThat(statuses).containsExactly(OK.getCode(), OK.getCode());
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
			return 3600;
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
	PostCollectionHook collectionHook() {
		return () -> {};
	}
}
