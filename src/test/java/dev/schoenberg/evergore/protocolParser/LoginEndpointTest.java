package dev.schoenberg.evergore.protocolParser;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.zip.GZIPOutputStream;

import jakarta.inject.Inject;

import io.micronaut.context.ApplicationContext;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.schoenberg.evergore.protocolParser.application.AccountRepository;
import dev.schoenberg.evergore.protocolParser.application.EvergoreDataExtractor;
import dev.schoenberg.evergore.protocolParser.application.PasswordHasher;
import dev.schoenberg.evergore.protocolParser.application.SignedLogins;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.Account;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.Login;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.PasswordState;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.Role;
import dev.schoenberg.evergore.protocolParser.dataExtraction.PostCollectionHook;
import dev.schoenberg.evergore.protocolParser.database.PreDatabaseConnectionHook;
import dev.schoenberg.evergore.protocolParser.helper.config.Configuration;

import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.silentThrow;
import static io.micronaut.http.HttpStatus.OK;
import static io.micronaut.http.HttpStatus.REQUEST_ENTITY_TOO_LARGE;
import static io.micronaut.http.HttpStatus.UNAUTHORIZED;
import static org.assertj.core.api.Assertions.assertThat;

@MicronautTest
class LoginEndpointTest {
	private static final Path WORKING_DB = Paths.get("build/tmp/login/login.sqlite");
	private static final Instant NOW = Instant.parse("2026-01-05T10:00:00Z");
	private static final String CHOSEN_PASSWORD = "Sommer-2026!";
	private static final String ISSUED_PASSWORD = "Issued-2026!";
	private static final String SESSION_COOKIE = "epc_session=[^;]+; Max-Age=604800; Path=/; HttpOnly; SameSite=Lax";
	private static final String SET_COOKIE = "Set-Cookie";
	private static final int FIVE_KIBIBYTES = 5 * 1024;

	static {
		silentThrow(() -> {
			Files.createDirectories(WORKING_DB.getParent());
			Files.deleteIfExists(WORKING_DB);
			try (InputStream src = LoginEndpointTest.class.getResourceAsStream("/testdata.sqlite")) {
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
		holdAccount("Aurora", CHOSEN_PASSWORD, PasswordState.CHOSEN, Role.MEMBER);
		holdAccount("Boreas", ISSUED_PASSWORD, PasswordState.ISSUED, Role.MEMBER);
		holdAccount("Calix", CHOSEN_PASSWORD, PasswordState.CHOSEN, Role.ADMIN);
	}

	@Test
	void answersTheRightChosenPasswordWithTheLoggedInStep() {
		HttpResponse<String> response = logIn("Aurora", CHOSEN_PASSWORD);

		assertThat(response.getStatus()).isEqualTo(OK.getCode());
		assertThat(response.getBody()).isEqualTo("{\"step\":\"logged-in\"}");
	}

	@Test
	void letsALoginThroughWithoutTheGuildToken() {
		HttpResponse<String> response = logIn("Aurora", CHOSEN_PASSWORD);

		assertThat(response.getStatus()).as("the login must be public").isEqualTo(OK.getCode());
	}

	@Test
	void setsTheSessionCookieForSevenDaysWithoutSecureOrExpires() {
		HttpResponse<String> response = logIn("Aurora", CHOSEN_PASSWORD);

		String cookie = response.getHeaders().getFirst(SET_COOKIE);

		assertThat(cookie).matches(SESSION_COOKIE);
	}

	@Test
	void putsALoginOfTheAccountThatEndsSevenDaysAfterItWasMadeIntoTheCookie() {
		HttpResponse<String> response = logIn("aurora", CHOSEN_PASSWORD);

		String cookie = response.getHeaders().getFirst(SET_COOKIE);
		Optional<Login> login = signedLogins().read(valueOf(cookie));

		assertThat(login).map(Login::accountName).contains("Aurora");
		assertThat(login).map(Login::role).contains(Role.MEMBER);
		assertThat(login).map(Login::expiresAt).contains(NOW.plus(Duration.ofDays(7)));
	}

	@Test
	void recordsTheMomentOfTheLoginAsTheLastLogin() {
		logIn("Calix", CHOSEN_PASSWORD);

		Optional<Instant> lastLogin = accounts().findByNameKey("calix").flatMap(Account::lastLogin);

		assertThat(lastLogin).contains(NOW);
	}

	@Test
	void asksForANewPasswordWhereTheIssuedPasswordIsRightAndSetsNoCookie() {
		HttpResponse<String> response = logIn("Boreas", ISSUED_PASSWORD);

		assertThat(response.getStatus()).isEqualTo(OK.getCode());
		assertThat(response.getBody()).isEqualTo("{\"step\":\"choose-new-password\"}");
		assertThat(response.getHeaders().containsKey(SET_COOKIE)).isFalse();
	}

	@Test
	void refusesAnUnknownNameWithTheSameAnswerAsAWrongPassword() {
		HttpResponse<String> unknownName = logIn("Zephyr", CHOSEN_PASSWORD);
		HttpResponse<String> wrongPassword = logIn("Aurora", "Winter-2026!");

		assertThat(unknownName.getStatus()).isEqualTo(UNAUTHORIZED.getCode());
		assertThat(wrongPassword.getStatus()).isEqualTo(UNAUTHORIZED.getCode());
		assertThat(wrongPassword.getBody()).isEqualTo("{\"refusal\":\"wrong-name-or-password\"}");
		assertThat(unknownName.getBody()).isEqualTo(wrongPassword.getBody());
		assertThat(unknownName.getHeaders().containsKey(SET_COOKIE)).isFalse();
		assertThat(wrongPassword.getHeaders().containsKey(SET_COOKIE)).isFalse();
	}

	@Test
	void refusesABodyWithoutAPasswordAsAWrongPassword() {
		HttpResponse<String> response = post("{\"name\":\"Aurora\"}");

		assertThat(response.getStatus()).isEqualTo(UNAUTHORIZED.getCode());
		assertThat(response.getBody()).isEqualTo("{\"refusal\":\"wrong-name-or-password\"}");
	}

	@Test
	void refusesABodyOverFourKibibytesWith413() {
		HttpResponse<String> response = logIn("Aurora", "x".repeat(FIVE_KIBIBYTES));

		assertThat(response.getStatus()).isEqualTo(REQUEST_ENTITY_TOO_LARGE.getCode());
	}

	@Test
	void refusesABodyOverFourKibibytesWith413WhenThePathStartsWithTwoSlashes() {
		String body = "x".repeat(FIVE_KIBIBYTES);
		String request = "POST //api/v1/login HTTP/1.1\r\nHost: localhost\r\nContent-Type: application/json\r\nContent-Length: " + body.length() + "\r\nConnection: close\r\n\r\n"
				+ body;

		int status = new RawHttpClient(server.getPort()).statusOfRequest(request);

		assertThat(status).isEqualTo(REQUEST_ENTITY_TOO_LARGE.getCode());
	}

	@Test
	void refusesAGzippedBodyThatDecodesToMoreThanFourKibibytesWith413() {
		byte[] request = chunkedRequest("Transfer-Encoding: gzip, chunked", gzipped(loginBodyWithAPasswordOf(FIVE_KIBIBYTES)));

		int status = new RawHttpClient(server.getPort()).statusOfRequest(request);

		assertThat(status).isEqualTo(REQUEST_ENTITY_TOO_LARGE.getCode());
	}

	@Test
	void refusesAChunkedBodyNamedInASecondTransferEncodingLineWith413() {
		byte[] request = chunkedRequest("Transfer-Encoding: identity\r\nTransfer-Encoding: chunked",
				loginBodyWithAPasswordOf(FIVE_KIBIBYTES + 1000).getBytes(StandardCharsets.US_ASCII));

		int status = new RawHttpClient(server.getPort()).statusOfRequest(request);

		assertThat(status).isEqualTo(REQUEST_ENTITY_TOO_LARGE.getCode());
	}

	@Test
	void refusesAGzippedBodyUnderAContentEncodingWith413() {
		byte[] zipped = gzipped(loginBodyWithAPasswordOf(FIVE_KIBIBYTES));
		byte[] head = ("POST /api/v1/login HTTP/1.1\r\nHost: localhost\r\nContent-Type: application/json\r\nContent-Encoding: gzip\r\nContent-Length: " + zipped.length
				+ "\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII);

		int status = new RawHttpClient(server.getPort()).statusOfRequest(concatenated(head, zipped));

		assertThat(status).isEqualTo(REQUEST_ENTITY_TOO_LARGE.getCode());
	}

	private static String loginBodyWithAPasswordOf(int length) {
		return "{\"name\":\"Aurora\",\"password\":\"" + "x".repeat(length) + "\"}";
	}

	private static byte[] gzipped(String text) {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		silentThrow(() -> {
			try (GZIPOutputStream gzip = new GZIPOutputStream(bytes)) {
				gzip.write(text.getBytes(StandardCharsets.US_ASCII));
			}
			return null;
		});
		return bytes.toByteArray();
	}

	private static byte[] chunkedRequest(String encodingLines, byte[] body) {
		byte[] head = ("POST /api/v1/login HTTP/1.1\r\nHost: localhost\r\nContent-Type: application/json\r\n" + encodingLines + "\r\nConnection: close\r\n\r\n"
				+ Integer.toHexString(body.length) + "\r\n").getBytes(StandardCharsets.US_ASCII);
		byte[] tail = "\r\n0\r\n\r\n".getBytes(StandardCharsets.US_ASCII);
		return concatenated(concatenated(head, body), tail);
	}

	private static byte[] concatenated(byte[] first, byte[] second) {
		byte[] both = new byte[first.length + second.length];
		System.arraycopy(first, 0, both, 0, first.length);
		System.arraycopy(second, 0, both, first.length, second.length);
		return both;
	}

	private HttpResponse<String> logIn(String name, String password) {
		return post(new JSONObject(Map.of("name", name, "password", password)).toString());
	}

	private static HttpResponse<String> post(String body) {
		return Unirest.post("/api/v1/login").header("Content-Type", "application/json").body(body).asString();
	}

	private static String valueOf(String setCookie) {
		return setCookie.substring(setCookie.indexOf('=') + 1, setCookie.indexOf(';'));
	}

	private void holdAccount(String avatar, String password, PasswordState state, Role role) {
		accounts().addIfAbsent(new Account(avatar, context.getBean(PasswordHasher.class).hash(password), state, role, Optional.empty()));
	}

	private AccountRepository accounts() {
		return context.getBean(AccountRepository.class);
	}

	private SignedLogins signedLogins() {
		return context.getBean(SignedLogins.class);
	}

	@MockBean(Clock.class)
	Clock clock() {
		return Clock.fixed(NOW, ZoneOffset.UTC);
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
