package dev.schoenberg.evergore.protocolParser;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import jakarta.inject.Inject;

import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.schoenberg.evergore.protocolParser.application.EvergoreDataExtractor;
import dev.schoenberg.evergore.protocolParser.dataExtraction.PostCollectionHook;
import dev.schoenberg.evergore.protocolParser.database.PreDatabaseConnectionHook;
import dev.schoenberg.evergore.protocolParser.helper.config.Configuration;

import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.silentThrow;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

@MicronautTest
class SpaHistoryFallbackTest {

	private static final Path WORKING_DB = Paths.get("build/tmp/spaHistoryFallback/spaHistoryFallback.sqlite");

	static {
		silentThrow(() -> {
			Files.createDirectories(WORKING_DB.getParent());
			Files.deleteIfExists(WORKING_DB);
			try (InputStream src = SpaHistoryFallbackTest.class.getResourceAsStream("/testdata.sqlite")) {
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
	void servesTheSpaShellForAnUnknownNavigationPath() {
		HttpResponse<String> response = getHtml("/some/client/route");

		assertThat(response.getStatus()).isEqualTo(200);
		assertThat(response.getBody()).isEqualTo(bundledIndexHtml());
		assertThat(response.getHeaders().getFirst("Cache-Control")).isEqualTo("no-cache");
	}

	@Test
	void servesTheSpaShellForAClientRouteWithADotInAMiddleSegment() {
		HttpResponse<String> response = getHtml("/avatars/Dr.Who/details");

		assertThat(response.getStatus()).isEqualTo(200);
		assertThat(response.getBody()).isEqualTo(bundledIndexHtml());
	}

	@Test
	void servesTheSpaShellForAClientRouteWhoseLastSegmentCarriesANonExtensionDot() {
		HttpResponse<String> response = getHtml("/avatars/Dr.Who");

		assertThat(response.getStatus()).isEqualTo(200);
		assertThat(response.getBody()).isEqualTo(bundledIndexHtml());
	}

	@Test
	void keepsTheDefaultNotFoundForAMissingAsset() {
		int status = getHtml("/assets/does-not-exist.js").getStatus();

		assertThat(status).isEqualTo(404);
	}

	@Test
	void keepsTheDefaultNotFoundForADotlessMissingAsset() {
		int status = getHtml("/assets/does-not-exist").getStatus();

		assertThat(status).isEqualTo(404);
	}

	@Test
	void keepsTheDefaultNotFoundForAMissingSwaggerPath() {
		int status = getHtml("/swagger/does-not-exist").getStatus();

		assertThat(status).isEqualTo(404);
	}

	@Test
	void keepsTheDefaultNotFoundForAnUnknownApiPath() {
		int status = getHtml("/api/does-not-exist").getStatus();

		assertThat(status).isEqualTo(404);
	}

	@Test
	void keepsTheDefaultNotFoundForATraversalThatResolvesBelowTheAssetsMapping() {
		int status = getHtml("/overview/../assets/does-not-exist").getStatus();

		assertThat(status).isEqualTo(404);
	}

	private HttpResponse<String> getHtml(String path) {
		return Unirest.get(path + "?token=test-token").header("Accept", "text/html").asString();
	}

	private String bundledIndexHtml() {
		return silentThrow(() -> {
			try (InputStream indexHtml = getClass().getResourceAsStream("/static/ui/index.html")) {
				return new String(indexHtml.readAllBytes(), UTF_8);
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
