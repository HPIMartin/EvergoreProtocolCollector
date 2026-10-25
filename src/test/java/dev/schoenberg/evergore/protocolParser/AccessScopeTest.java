package dev.schoenberg.evergore.protocolParser;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.inject.Inject;

import io.micronaut.context.ApplicationContext;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import dev.schoenberg.evergore.protocolParser.application.EvergoreDataExtractor;
import dev.schoenberg.evergore.protocolParser.application.SignedLogins;
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
class AccessScopeTest {
	private static final Path WORKING_DB = Paths.get("build/tmp/accessScope/accessScope.sqlite");
	private static final Pattern BUNDLED_ASSET = Pattern.compile("\"(/assets/[^\"]+)\"");
	private static final Pattern BUNDLED_LOGIN_FILE = Pattern.compile("\"(/login/assets/[^\"]+)\"");
	private static final String OLD_TOKEN = "test-token";
	private static final String SESSION_COOKIE = "epc_session";

	static {
		silentThrow(() -> {
			Files.createDirectories(WORKING_DB.getParent());
			Files.deleteIfExists(WORKING_DB);
			try (InputStream src = AccessScopeTest.class.getResourceAsStream("/testdata.sqlite")) {
				Files.copy(src, WORKING_DB);
			}
		});
	}

	private @Inject EmbeddedServer server;
	private @Inject ApplicationContext context;

	@BeforeEach
	void setup() {
		Unirest.config().verifySsl(false);
		Unirest.config().defaultBaseUrl("http://localhost:" + server.getPort());
	}

	@ParameterizedTest
	@ValueSource(strings = {"/api/v1/avatars", "/api/v1/avatars/Aurora/bank", "/api/v1/admin/status", "/api/v1/unknown", "/index.html", "/favicon.ico", "/swagger-ui/index.html",
			"/swagger/evergore-protocol-parser-1.0.0.yml", "/redoc/index.html", "/rapidoc/index.html", "/i_dont_exist", "//api/v1/avatars"})
	void refusesARequestWithoutALoginForEveryAddressThatIsNotPublic(String path) {
		HttpResponse<String> response = Unirest.get(path).asString();

		assertThat(response.getStatus()).isEqualTo(UNAUTHORIZED.getCode());
	}

	@Test
	void refusesTheBundledFileOfTheDashboardWithoutALogin() {
		HttpResponse<String> response = Unirest.get(bundledAssetPath()).asString();

		assertThat(response.getStatus()).isEqualTo(UNAUTHORIZED.getCode());
	}

	@ParameterizedTest
	@ValueSource(strings = {"/api/v1/avatars", "/api/v1/admin/status", "/index.html", "/health/../api/v1/avatars", "/login/../api/v1/avatars"})
	void opensNothingWithTheGuildsOldTokenInTheAddress(String path) {
		HttpResponse<String> response = Unirest.get(path + (path.contains("?") ? "&" : "?") + "token=" + OLD_TOKEN).asString();

		assertThat(response.getStatus()).isEqualTo(UNAUTHORIZED.getCode());
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = {"/overview|%2Foverview", "//overview|%2Foverview", "/|%2F", "/admin|%2Fadmin", "/explanation|%2Fexplanation",
			"/this-page-does-not-exist|%2Fthis-page-does-not-exist", "/avatars/Aurora/bank?page=2|%2Favatars%2FAurora%2Fbank%3Fpage%3D2", "/assets/../overview|%2Foverview",
			"/assets/%2e%2e/overview|%2Foverview", "/login/../overview|%2Foverview", "/login/%2e%2e/overview|%2Foverview", "/LOGIN/../overview|%2Foverview",
			"/health/../overview|%2Foverview", "/?token=old|%2F%3Ftoken%3Dold"})
	void sendsANavigationWithoutALoginToTheLoginPageWithThePageAsTheWayBack(String requestTarget, String wayBack) {
		String answer = new RawHttpClient(server.getPort()).answerUntilTheServerCloses(navigationTo(requestTarget));

		assertThat(answer).startsWith("HTTP/1.1 302");
		assertThat(answer.toLowerCase(Locale.ROOT)).contains("location: /login?next=" + wayBack.toLowerCase(Locale.ROOT));
	}

	@Test
	void refusesAnApiRequestWithoutALoginEvenWhenItAsksForHtml() {
		String answer = new RawHttpClient(server.getPort()).answerUntilTheServerCloses(navigationTo("/api/v1/avatars"));

		assertThat(answer).startsWith("HTTP/1.1 401");
	}

	@ParameterizedTest
	@ValueSource(strings = {"/overview%zz", "/overview%", "/overview%2", "/api/v1/avatars%zz", "/avatars/Aurora%zz/bank"})
	void answersAMalformedRequestTargetWith400InsteadOf401(String requestTarget) {
		int status = new RawHttpClient(server.getPort()).statusOf(requestTarget);

		assertThat(status).isEqualTo(BAD_REQUEST.getCode());
	}

	@ParameterizedTest
	@ValueSource(strings = {"/login", "/login/", "/health"})
	void servesAPublicAddressWithoutALogin(String path) {
		HttpResponse<String> response = Unirest.get(path).asString();

		assertThat(response.getStatus()).isEqualTo(OK.getCode());
	}

	@Test
	void servesTheLoginPagesOwnFilesWithoutALogin() {
		HttpResponse<String> response = Unirest.get(bundledLoginFilePath()).asString();

		assertThat(response.getStatus()).isEqualTo(OK.getCode());
	}

	@ParameterizedTest
	@ValueSource(strings = {"/api/v1/avatars", "/api/v1/admin/status", "/index.html", "/favicon.svg"})
	void opensWhatIsNotPublicForARequestWithALoginTheServiceIssued(String path) {
		HttpResponse<String> response = Unirest.get(path).header("Cookie", loginCookie()).asString();

		assertThat(response.getStatus()).isEqualTo(OK.getCode());
	}

	@Test
	void opensTheBundledFileOfTheDashboardForARequestWithALogin() {
		HttpResponse<String> response = Unirest.get(bundledAssetPath()).header("Cookie", loginCookie()).asString();

		assertThat(response.getStatus()).isEqualTo(OK.getCode());
	}

	@ParameterizedTest
	@ValueSource(strings = {"/", "/overview", "/avatars/Aurora/bank", "/some/client/route"})
	void servesTheShellOfTheDashboardAtAPageForARequestWithALogin(String path) {
		HttpResponse<String> response = Unirest.get(path).header("Accept", "text/html").header("Cookie", loginCookie()).asString();

		assertThat(response.getStatus()).isEqualTo(OK.getCode());
		assertThat(response.getBody()).isEqualTo(bundledIndexHtml());
	}

	private String loginCookie() {
		return SESSION_COOKIE + "=" + context.getBean(SignedLogins.class).issueForTheAdminKey("admin");
	}

	private static String navigationTo(String requestTarget) {
		return "GET " + requestTarget + " HTTP/1.1\r\nHost: localhost\r\nAccept: text/html\r\nConnection: close\r\n\r\n";
	}

	private static String bundledAssetPath() {
		Matcher asset = BUNDLED_ASSET.matcher(bundledIndexHtml());
		assertThat(asset.find()).as("the bundled index.html must reference at least one /assets/ file").isTrue();
		return asset.group(1);
	}

	private static String bundledLoginFilePath() {
		Matcher file = BUNDLED_LOGIN_FILE.matcher(bundledResource("/static/ui/login/login.html"));
		assertThat(file.find()).as("the bundled login page must reference at least one /login/assets/ file").isTrue();
		return file.group(1);
	}

	private static String bundledIndexHtml() {
		return bundledResource("/static/ui/index.html");
	}

	private static String bundledResource(String path) {
		return silentThrow(() -> {
			try (InputStream resource = AccessScopeTest.class.getResourceAsStream(path)) {
				return new String(resource.readAllBytes(), UTF_8);
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
