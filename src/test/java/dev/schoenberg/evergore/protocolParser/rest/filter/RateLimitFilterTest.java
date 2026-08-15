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
import static io.micronaut.http.HttpStatus.REQUEST_ENTITY_TOO_LARGE;
import static io.micronaut.http.HttpStatus.TOO_MANY_REQUESTS;
import static io.micronaut.http.HttpStatus.UNAUTHORIZED;
import static org.assertj.core.api.Assertions.assertThat;

@MicronautTest(environments = "ratelimit", rebuildContext = true)
class RateLimitFilterTest {

	private static final Path WORKING_DB = Paths.get("build/tmp/rateLimit/rateLimit.sqlite");
	private static final int REQUESTS_PER_BURST = 3;
	private static final int TARGET_LONGER_THAN_THE_SERVER_ACCEPTS = 5000;

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
	void countsTokenlessRequestsToTheSpaShellRoot() {
		List<Integer> statuses = statusesOfThreeRequestsTo("/");

		assertThat(statuses).containsExactly(OK.getCode(), OK.getCode(), TOO_MANY_REQUESTS.getCode());
	}

	@Test
	void countsTokenlessRequestsToIndexHtml() {
		List<Integer> statuses = statusesOfThreeRequestsTo("/index.html");

		assertThat(statuses).containsExactly(OK.getCode(), OK.getCode(), TOO_MANY_REQUESTS.getCode());
	}

	@Test
	void countsTokenlessRequestsToBundledAssets() {
		String assetPath = bundledAssetPath();

		List<Integer> statuses = statusesOfThreeRequestsTo(assetPath);

		assertThat(statuses).containsExactly(OK.getCode(), OK.getCode(), TOO_MANY_REQUESTS.getCode());
	}

	@Test
	void countsADoubleSlashPathInsteadOfMistakingItForTheSpaRoot() {
		List<Integer> statuses = statusesOfThreeRequestsTo("//probe");

		assertThat(statuses).containsExactly(UNAUTHORIZED.getCode(), UNAUTHORIZED.getCode(), TOO_MANY_REQUESTS.getCode());
	}

	@Test
	void countsAMalformedRequestTargetLikeAnyOther() {
		RawHttpClient rawClient = new RawHttpClient(server.getPort());

		List<Integer> statuses = IntStream.range(0, REQUESTS_PER_BURST).mapToObj(request -> rawClient.statusOf("/overview%zz")).toList();

		assertThat(statuses).containsExactly(BAD_REQUEST.getCode(), BAD_REQUEST.getCode(), TOO_MANY_REQUESTS.getCode());
	}

	@Test
	void answersAnOversizedRequestTargetWithoutCountingIt() {
		RawHttpClient rawClient = new RawHttpClient(server.getPort());
		String oversizedTarget = "/" + "a".repeat(TARGET_LONGER_THAN_THE_SERVER_ACCEPTS);

		List<Integer> statuses = IntStream.range(0, REQUESTS_PER_BURST).mapToObj(request -> rawClient.statusOf(oversizedTarget)).toList();

		assertThat(statuses).as("a 429 on the third request would mean the counter saw them; 413 throughout means it did not").containsOnly(REQUEST_ENTITY_TOO_LARGE.getCode());
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
