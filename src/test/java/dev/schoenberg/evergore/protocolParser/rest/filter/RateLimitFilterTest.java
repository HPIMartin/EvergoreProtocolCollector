package dev.schoenberg.evergore.protocolParser.rest.filter;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import jakarta.inject.Inject;

import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import kong.unirest.Unirest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.schoenberg.evergore.protocolParser.RawHttpClient;
import dev.schoenberg.evergore.protocolParser.application.EvergoreDataExtractor;
import dev.schoenberg.evergore.protocolParser.dataExtraction.PostCollectionHook;
import dev.schoenberg.evergore.protocolParser.database.PreDatabaseConnectionHook;
import dev.schoenberg.evergore.protocolParser.helper.config.Configuration;

import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.silentThrow;
import static io.micronaut.http.HttpStatus.BAD_REQUEST;
import static io.micronaut.http.HttpStatus.OK;
import static io.micronaut.http.HttpStatus.TOO_MANY_REQUESTS;
import static io.micronaut.http.HttpStatus.UNAUTHORIZED;
import static org.assertj.core.api.Assertions.assertThat;

@MicronautTest(environments = "ratelimit", rebuildContext = true)
class RateLimitFilterTest {

	private static final Path WORKING_DB = Paths.get("build/tmp/rateLimit/rateLimit.sqlite");
	private static final int REQUESTS_PER_BURST = 3;

	static {
		silentThrow(() -> {
			Files.createDirectories(WORKING_DB.getParent());
			Files.deleteIfExists(WORKING_DB);
			try (InputStream src = RateLimitFilterTest.class.getResourceAsStream("/testdata.sqlite")) {
				Files.copy(src, WORKING_DB);
			}
		});
	}

	private @Inject EmbeddedServer server;

	@BeforeEach
	void setup() {
		Unirest.config().verifySsl(false);
		Unirest.config().defaultBaseUrl("http://localhost:" + server.getPort());
	}

	@Test
	void blocksRequestsOnceTheConfiguredLimitIsExceeded() {
		List<Integer> statuses = statusesOfThreeRequestsTo("/favicon.ico");

		assertThat(statuses).containsExactly(OK.getCode(), OK.getCode(), TOO_MANY_REQUESTS.getCode());
	}

	@Test
	void allowsRepeatedTokenlessRequestsToTheSpaShellRoot() {
		List<Integer> statuses = statusesOfThreeRequestsTo("/");

		assertThat(statuses).containsOnly(OK.getCode());
	}

	@Test
	void allowsRepeatedTokenlessRequestsToIndexHtml() {
		List<Integer> statuses = statusesOfThreeRequestsTo("/index.html");

		assertThat(statuses).containsOnly(OK.getCode());
	}

	@Test
	void allowsRepeatedTokenlessRequestsToBundledAssets() {
		String assetPath = bundledAssetPath();

		List<Integer> statuses = statusesOfThreeRequestsTo(assetPath);

		assertThat(statuses).containsOnly(OK.getCode());
	}

	@Test
	void countsADoubleSlashPathInsteadOfMistakingItForTheSpaRoot() {
		List<Integer> statuses = statusesOfThreeRequestsTo("//probe");

		assertThat(statuses).containsExactly(UNAUTHORIZED.getCode(), UNAUTHORIZED.getCode(), TOO_MANY_REQUESTS.getCode());
	}

	@Test
	void doesNotCountAMalformedRequestTargetBecauseTheFrameworkAnswersItFirst() {
		RawHttpClient rawClient = new RawHttpClient(server.getPort());

		List<Integer> statuses = IntStream.range(0, REQUESTS_PER_BURST).mapToObj(request -> rawClient.statusOf("/overview%zz")).toList();

		assertThat(statuses).as("Micronaut answers a malformed target itself, so the counter never sees it; a 429 would mean it did").containsOnly(BAD_REQUEST.getCode());
	}

	private List<Integer> statusesOfThreeRequestsTo(String path) {
		return IntStream.range(0, REQUESTS_PER_BURST).mapToObj(request -> statusOf(path)).toList();
	}

	private int statusOf(String path) {
		return Unirest.get(path).asString().getStatus();
	}

	private String bundledAssetPath() {
		return silentThrow(() -> {
			URL assetsUrl = getClass().getResource("/static/ui/assets");
			try (Stream<Path> files = Files.list(Paths.get(assetsUrl.toURI()))) {
				Path asset = files.filter(path -> path.getFileName().toString().endsWith(".js")).findFirst().orElseThrow();
				return "/assets/" + asset.getFileName();
			}
		});
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
