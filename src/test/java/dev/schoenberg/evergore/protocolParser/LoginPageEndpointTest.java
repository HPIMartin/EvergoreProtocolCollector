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
import static io.micronaut.http.HttpStatus.NOT_FOUND;
import static io.micronaut.http.HttpStatus.OK;
import static io.micronaut.http.HttpStatus.UNAUTHORIZED;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

@MicronautTest
class LoginPageEndpointTest {
	private static final Path WORKING_DB = Paths.get("build/tmp/loginPage/loginPage.sqlite");
	private static final Pattern BUNDLED_FILE = Pattern.compile("\"(/login/assets/[^\"]+)\"");

	static {
		silentThrow(() -> {
			Files.createDirectories(WORKING_DB.getParent());
			Files.deleteIfExists(WORKING_DB);
			try (InputStream src = LoginPageEndpointTest.class.getResourceAsStream("/testdata.sqlite")) {
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

	@ParameterizedTest
	@ValueSource(strings = {"/login", "/login/"})
	void servesTheLoginPageWithoutAToken(String path) {
		HttpResponse<String> response = navigateTo(path);

		assertThat(response.getStatus()).isEqualTo(OK.getCode());
		assertThat(response.getBody()).isEqualTo(bundledLoginPage());
	}

	@Test
	void servesTheLoginPagesOwnFilesWithoutAToken() {
		HttpResponse<String> response = Unirest.get(bundledFilePath()).asString();

		assertThat(response.getStatus()).isEqualTo(OK.getCode());
	}

	@Test
	void sendsADashboardPageOpenedWithoutALoginOnToTheLoginPage() {
		HttpResponse<String> response = navigateTo("/overview");

		assertThat(response.getStatus()).isEqualTo(OK.getCode());
		assertThat(response.getBody()).isEqualTo(bundledLoginPage());
	}

	@Test
	void refusesAnApiRequestWithoutALoginInsteadOfSendingItOnToTheLoginPage() {
		HttpResponse<String> response = Unirest.get("/api/v1/avatars").asString();

		assertThat(response.getStatus()).isEqualTo(UNAUTHORIZED.getCode());
	}

	@Test
	void answersAnUnknownPathBelowTheLoginPageWithoutTheDashboard() {
		HttpResponse<String> response = navigateTo("/login/unknown");

		assertThat(response.getStatus()).isEqualTo(NOT_FOUND.getCode());
	}

	private static HttpResponse<String> navigateTo(String path) {
		return Unirest.get(path).header("Accept", "text/html").asString();
	}

	private static String bundledFilePath() {
		Matcher file = BUNDLED_FILE.matcher(bundledLoginPage());
		assertThat(file.find()).as("the bundled login page must reference at least one /login/assets/ file").isTrue();
		return file.group(1);
	}

	private static String bundledLoginPage() {
		return silentThrow(() -> {
			try (InputStream page = LoginPageEndpointTest.class.getResourceAsStream("/static/ui/login/login.html")) {
				assertThat(page).as("the jar must carry the login page").isNotNull();
				return new String(page.readAllBytes(), UTF_8);
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
