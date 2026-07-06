package dev.schoenberg.evergore.protocolParser.rest.filter;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import jakarta.inject.Inject;

import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import kong.unirest.Unirest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.schoenberg.evergore.protocolParser.application.EvergoreDataExtractor;
import dev.schoenberg.evergore.protocolParser.dataExtraction.PostCollectionHook;
import dev.schoenberg.evergore.protocolParser.database.PreDatabaseConnectionHook;
import dev.schoenberg.evergore.protocolParser.helper.config.Configuration;

import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.silentThrow;
import static org.assertj.core.api.Assertions.assertThat;

@MicronautTest(environments = "ratelimit", rebuildContext = true)
class RateLimitFilterTest {

	private static final Path WORKING_DB = Paths.get("build/tmp/rateLimit/rateLimit.sqlite");

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
		assertThat(status("/favicon.ico")).isEqualTo(200);
		assertThat(status("/favicon.ico")).isEqualTo(200);
		assertThat(status("/favicon.ico")).isEqualTo(429);
	}

	@Test
	void allowsRepeatedTokenlessRequestsToTheSpaShellRoot() {
		assertThat(status("/")).isEqualTo(200);
		assertThat(status("/")).isEqualTo(200);
		assertThat(status("/")).isEqualTo(200);
	}

	@Test
	void allowsRepeatedTokenlessRequestsToIndexHtml() {
		assertThat(status("/index.html")).isEqualTo(200);
		assertThat(status("/index.html")).isEqualTo(200);
		assertThat(status("/index.html")).isEqualTo(200);
	}

	@Test
	void allowsRepeatedTokenlessRequestsToBundledAssets() {
		String assetPath = bundledAssetPath();

		assertThat(status(assetPath)).isEqualTo(200);
		assertThat(status(assetPath)).isEqualTo(200);
		assertThat(status(assetPath)).isEqualTo(200);
	}

	@Test
	void stillRejectsTheOverviewEndpointWithoutAToken() {
		assertThat(status("/overview")).isEqualTo(401);
	}

	private int status(String path) {
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
