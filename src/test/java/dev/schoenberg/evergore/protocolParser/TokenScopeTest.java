package dev.schoenberg.evergore.protocolParser;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.inject.Inject;

import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import dev.schoenberg.evergore.protocolParser.application.EvergoreDataExtractor;
import dev.schoenberg.evergore.protocolParser.dataExtraction.PostCollectionHook;
import dev.schoenberg.evergore.protocolParser.database.PreDatabaseConnectionHook;
import dev.schoenberg.evergore.protocolParser.helper.config.Configuration;

import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.silentThrow;
import static io.micronaut.http.HttpStatus.BAD_REQUEST;
import static io.micronaut.http.HttpStatus.OK;
import static io.micronaut.http.HttpStatus.UNAUTHORIZED;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

@MicronautTest
class TokenScopeTest {
	private static final Path WORKING_DB = Paths.get("build/tmp/tokenScope/tokenScope.sqlite");
	private static final Pattern BUNDLED_ASSET = Pattern.compile("\"(/assets/[^\"]+)\"");

	static {
		silentThrow(() -> {
			Files.createDirectories(WORKING_DB.getParent());
			Files.deleteIfExists(WORKING_DB);
			try (InputStream src = TokenScopeTest.class.getResourceAsStream("/testdata.sqlite")) {
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
	void rejectsAnApiRequestWithoutAToken() {
		int status = statusWithoutToken("/api/v1/avatars");

		assertThat(status).isEqualTo(UNAUTHORIZED.getCode());
	}

	@Test
	void rejectsAnApiRequestWithAWrongToken() {
		int status = statusOf(Unirest.get("/api/v1/avatars?token=wrong").asString());

		assertThat(status).isEqualTo(UNAUTHORIZED.getCode());
	}

	@Test
	void rejectsTheLegacyOverviewPageWithoutAToken() {
		int status = statusWithoutToken("/overview");

		assertThat(status).isEqualTo(UNAUTHORIZED.getCode());
	}

	@Test
	void rejectsTheLegacyAvatarPageWithoutAToken() {
		int status = statusWithoutToken("/avatars/Aurora/bank");

		assertThat(status).isEqualTo(UNAUTHORIZED.getCode());
	}

	@Test
	void rejectsAnUnmappedPathWithoutAToken() {
		int status = statusWithoutToken("/i_dont_exist");

		assertThat(status).isEqualTo(UNAUTHORIZED.getCode());
	}

	@Test
	void rejectsASpaClientRouteWithoutAToken() {
		int status = statusWithoutToken("/some/client/route");

		assertThat(status).isEqualTo(UNAUTHORIZED.getCode());
	}

	@Test
	void rejectsATraversalThatResolvesOntoAProtectedPath() {
		int status = statusWithoutToken("/assets/../overview");

		assertThat(status).isEqualTo(UNAUTHORIZED.getCode());
	}

	@Test
	void rejectsAPercentEncodedTraversalThatResolvesOntoAProtectedPath() {
		int status = statusWithoutToken("/assets/%2e%2e/overview");

		assertThat(status).isEqualTo(UNAUTHORIZED.getCode());
	}

	@Test
	void rejectsADoubleSlashPathThatResolvesOntoAProtectedPage() {
		int status = statusWithoutToken("//overview");

		assertThat(status).isEqualTo(UNAUTHORIZED.getCode());
	}

	@Test
	void rejectsADoubleSlashApiPathWithoutAToken() {
		int status = statusWithoutToken("//api/v1/avatars");

		assertThat(status).isEqualTo(UNAUTHORIZED.getCode());
	}

	@ParameterizedTest
	@ValueSource(strings = {"/overview%zz", "/overview%", "/overview%2", "/api/v1/avatars%zz", "/avatars/Aurora%zz/bank"})
	void answersAMalformedRequestTargetBeforeTheTokenFilterCanSeeIt(String requestTarget) {
		int status = new RawHttpClient(server.getPort()).statusOf(requestTarget);

		assertThat(status).as("Micronaut rejects a malformed target itself; reaching the filter would make this a 401").isEqualTo(BAD_REQUEST.getCode());
	}

	@Test
	void servesTheLegacyOverviewPageWithAValidToken() {
		int status = statusOf(Unirest.get("/overview?token=test-token").asString());

		assertThat(status).isEqualTo(OK.getCode());
	}

	@Test
	void servesTheSpaShellWithoutAToken() {
		HttpResponse<String> response = getWithoutToken("/");

		assertThat(response.getStatus()).isEqualTo(OK.getCode());
		assertThat(response.getBody()).isEqualTo(bundledIndexHtml());
	}

	@Test
	void servesTheSpaIndexWithoutAToken() {
		int status = statusWithoutToken("/index.html");

		assertThat(status).isEqualTo(OK.getCode());
	}

	@Test
	void servesABundledAssetWithoutAToken() {
		int status = statusWithoutToken(bundledAssetPath());

		assertThat(status).isEqualTo(OK.getCode());
	}

	@Test
	void servesTheSwaggerUiWithoutAToken() {
		int status = statusWithoutToken("/swagger-ui/index.html");

		assertThat(status).isEqualTo(OK.getCode());
	}

	@Test
	void servesTheHealthEndpointWithoutAToken() {
		int status = statusWithoutToken("/health");

		assertThat(status).isEqualTo(OK.getCode());
	}

	@Test
	void servesTheFaviconWithoutAToken() {
		int status = statusWithoutToken("/favicon.ico");

		assertThat(status).isEqualTo(OK.getCode());
	}

	private static int statusWithoutToken(String path) {
		return statusOf(getWithoutToken(path));
	}

	private static int statusOf(HttpResponse<String> response) {
		return response.getStatus();
	}

	private static HttpResponse<String> getWithoutToken(String path) {
		return Unirest.get(path).asString();
	}

	private static String bundledAssetPath() {
		Matcher asset = BUNDLED_ASSET.matcher(bundledIndexHtml());
		assertThat(asset.find()).as("the bundled index.html must reference at least one /assets/ file").isTrue();
		return asset.group(1);
	}

	private static String bundledIndexHtml() {
		return silentThrow(() -> {
			try (InputStream indexHtml = TokenScopeTest.class.getResourceAsStream("/static/ui/index.html")) {
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
