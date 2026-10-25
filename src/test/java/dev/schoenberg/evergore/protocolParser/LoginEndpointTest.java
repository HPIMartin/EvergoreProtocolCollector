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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.GZIPOutputStream;

import jakarta.inject.Inject;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Property;
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
import dev.schoenberg.evergore.protocolParser.businessLogic.account.LoginSource;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.PasswordState;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.Role;
import dev.schoenberg.evergore.protocolParser.dataExtraction.PostCollectionHook;
import dev.schoenberg.evergore.protocolParser.database.PreDatabaseConnectionHook;
import dev.schoenberg.evergore.protocolParser.helper.config.Configuration;

import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.silentThrow;
import static io.micronaut.http.HttpStatus.BAD_REQUEST;
import static io.micronaut.http.HttpStatus.FORBIDDEN;
import static io.micronaut.http.HttpStatus.NO_CONTENT;
import static io.micronaut.http.HttpStatus.OK;
import static io.micronaut.http.HttpStatus.REQUEST_ENTITY_TOO_LARGE;
import static io.micronaut.http.HttpStatus.UNAUTHORIZED;
import static org.assertj.core.api.Assertions.assertThat;

@MicronautTest
@Property(name = FixedAdminKeyFactory.FIXED_ADMIN_KEY, value = "true")
class LoginEndpointTest {
	private static final Path WORKING_DB = Paths.get("build/tmp/login/login.sqlite");
	private static final Instant NOW = Instant.parse("2026-01-05T10:00:00Z");
	private static final String CHOSEN_PASSWORD = "Sommer-2026!";
	private static final String ISSUED_PASSWORD = "Issued-2026!";
	private static final String SESSION_COOKIE = "epc_session=[^;]+; Max-Age=604800; Path=/; HttpOnly; SameSite=Lax";
	private static final String SET_COOKIE = "Set-Cookie";
	private static final int FIVE_KIBIBYTES = 5 * 1024;
	private static final String PROTECTED_PATH = "/api/v1/avatars";

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
		assertThat(response.getBody()).isEqualTo("{\"step\":\"logged-in\",\"next\":\"/\"}");
	}

	@Test
	void letsALoginThroughWithoutALogin() {
		HttpResponse<String> response = logIn("Aurora", CHOSEN_PASSWORD);

		assertThat(response.getStatus()).as("the login must be public").isEqualTo(OK.getCode());
	}

	@Test
	void opensAProtectedPathWithTheLoginTheServiceIssued() {
		String login = valueOf(logIn("Aurora", CHOSEN_PASSWORD).getHeaders().getFirst(SET_COOKIE));

		HttpResponse<String> response = Unirest.get(PROTECTED_PATH).header("Cookie", "epc_session=" + login).asString();

		assertThat(response.getStatus()).isEqualTo(OK.getCode());
	}

	@Test
	void opensNothingProtectedWithALoginTheServiceDidNotIssue() {
		HttpResponse<String> response = Unirest.get(PROTECTED_PATH).header("Cookie", "epc_session=forged").asString();

		assertThat(response.getStatus()).isEqualTo(UNAUTHORIZED.getCode());
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

	@Test
	void logsInWithTheNewPasswordChosenAfterTheIssuedOne() {
		holdAccount("Dorian", ISSUED_PASSWORD, PasswordState.ISSUED, Role.MEMBER);

		HttpResponse<String> response = chooseNewPassword("dorian", ISSUED_PASSWORD, CHOSEN_PASSWORD);

		assertThat(response.getStatus()).isEqualTo(OK.getCode());
		assertThat(response.getBody()).isEqualTo("{\"step\":\"logged-in\",\"next\":\"/\"}");
		assertThat(response.getHeaders().getFirst(SET_COOKIE)).matches(SESSION_COOKIE);
	}

	@Test
	void logsInWithTheChosenPasswordFromThenOn() {
		holdAccount("Elara", ISSUED_PASSWORD, PasswordState.ISSUED, Role.MEMBER);
		chooseNewPassword("Elara", ISSUED_PASSWORD, CHOSEN_PASSWORD);

		HttpResponse<String> withTheNewPassword = logIn("Elara", CHOSEN_PASSWORD);
		HttpResponse<String> withTheIssuedPassword = logIn("Elara", ISSUED_PASSWORD);

		assertThat(withTheNewPassword.getBody()).isEqualTo("{\"step\":\"logged-in\",\"next\":\"/\"}");
		assertThat(withTheIssuedPassword.getStatus()).isEqualTo(UNAUTHORIZED.getCode());
	}

	@Test
	void refusesAWrongIssuedPasswordAnUnknownNameAndAChosenAccountWithTheAnswerOfAWrongPassword() {
		HttpResponse<String> wrongIssued = chooseNewPassword("Boreas", "Winter-2026!", "Herbst-2026!");
		HttpResponse<String> unknownName = chooseNewPassword("Zephyr", ISSUED_PASSWORD, "Herbst-2026!");
		HttpResponse<String> alreadyChosen = chooseNewPassword("Aurora", CHOSEN_PASSWORD, "Herbst-2026!");

		assertThat(List.of(wrongIssued.getStatus(), unknownName.getStatus(), alreadyChosen.getStatus())).containsOnly(UNAUTHORIZED.getCode());
		assertThat(List.of(wrongIssued.getBody(), unknownName.getBody(), alreadyChosen.getBody())).containsOnly("{\"refusal\":\"wrong-name-or-password\"}");
		assertThat(alreadyChosen.getHeaders().containsKey(SET_COOKIE)).isFalse();
	}

	@Test
	void refusesANewPasswordThatBreaksTheRuleWith400() {
		HttpResponse<String> response = chooseNewPassword("Boreas", ISSUED_PASSWORD, "Abc-123");

		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST.getCode());
		assertThat(response.getBody()).isEqualTo("{\"refusal\":\"breaks-the-rule\"}");
		assertThat(response.getHeaders().containsKey(SET_COOKIE)).isFalse();
	}

	@Test
	void refusesTheIssuedPasswordAsTheNewOneWith400() {
		HttpResponse<String> response = chooseNewPassword("Boreas", ISSUED_PASSWORD, ISSUED_PASSWORD);

		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST.getCode());
		assertThat(response.getBody()).isEqualTo("{\"refusal\":\"same-as-before\"}");
	}

	@Test
	void leavesTheIssuedPasswordValidAfterARefusedChoice() {
		chooseNewPassword("Boreas", ISSUED_PASSWORD, "Abcd1234");

		HttpResponse<String> response = logIn("Boreas", ISSUED_PASSWORD);

		assertThat(response.getBody()).isEqualTo("{\"step\":\"choose-new-password\"}");
	}

	@Test
	void refusesABodyOverFourKibibytesForTheNewPasswordWith413() {
		HttpResponse<String> response = chooseNewPassword("Boreas", ISSUED_PASSWORD, "x".repeat(FIVE_KIBIBYTES));

		assertThat(response.getStatus()).isEqualTo(REQUEST_ENTITY_TOO_LARGE.getCode());
	}

	@Test
	void logsInTheAdminKeyUnderTheNameAdminWithALoginOfTheKey() {
		HttpResponse<String> response = logIn("ADMIN", FixedRandom.ADMIN_KEY_OF_ONES);

		Optional<Login> login = signedLogins().read(valueOf(response.getHeaders().getFirst(SET_COOKIE)));

		assertThat(response.getBody()).isEqualTo("{\"step\":\"logged-in\",\"next\":\"/\"}");
		assertThat(login).map(Login::accountName).contains("admin");
		assertThat(login).map(Login::role).contains(Role.ADMIN);
		assertThat(login).map(Login::source).contains(LoginSource.ADMIN_KEY);
	}

	@Test
	void refusesTheSecondStepForTheNameAdminWithTheAnswerOfAWrongPassword() {
		HttpResponse<String> response = chooseNewPassword("admin", FixedRandom.ADMIN_KEY_OF_ONES, "Winter-2026!");

		assertThat(response.getStatus()).isEqualTo(UNAUTHORIZED.getCode());
		assertThat(response.getBody()).isEqualTo("{\"refusal\":\"wrong-name-or-password\"}");
	}

	@Test
	void answersTheLoginWithThePageOfThisServiceItWasAskedToReturnTo() {
		HttpResponse<String> response = logIn("Aurora", CHOSEN_PASSWORD, "/avatars/Aurora/bank?page=2");

		assertThat(response.getBody()).isEqualTo("{\"step\":\"logged-in\",\"next\":\"/avatars/Aurora/bank?page=2\"}");
	}

	@Test
	void answersTheLoginWithTheOverviewWhenAskedToReturnToAnotherSite() {
		HttpResponse<String> response = logIn("Aurora", CHOSEN_PASSWORD, "//evil.example/overview");

		assertThat(response.getBody()).isEqualTo("{\"step\":\"logged-in\",\"next\":\"/\"}");
	}

	@Test
	void answersTheNewPasswordWithThePageOfThisServiceItWasAskedToReturnTo() {
		holdAccount("Fenja", ISSUED_PASSWORD, PasswordState.ISSUED, Role.MEMBER);

		HttpResponse<String> response = post("/api/v1/login/new-password",
				new JSONObject(Map.of("name", "Fenja", "issuedPassword", ISSUED_PASSWORD, "newPassword", CHOSEN_PASSWORD, "next", "/avatars/Fenja/storage")).toString());

		assertThat(response.getBody()).isEqualTo("{\"step\":\"logged-in\",\"next\":\"/avatars/Fenja/storage\"}");
	}

	@Test
	void asksForANewPasswordWithoutAnAddressToReturnTo() {
		HttpResponse<String> response = logIn("Boreas", ISSUED_PASSWORD, "/overview");

		assertThat(response.getBody()).isEqualTo("{\"step\":\"choose-new-password\"}");
	}

	@Test
	void namesTheAccountOfALoginAndSaysItCanChangeItsPassword() {
		String login = valueOf(logIn("aurora", CHOSEN_PASSWORD).getHeaders().getFirst(SET_COOKIE));

		HttpResponse<String> response = Unirest.get("/api/v1/me").header("Cookie", "epc_session=" + login).asString();

		assertThat(response.getStatus()).isEqualTo(OK.getCode());
		assertThat(response.getBody()).isEqualTo("{\"name\":\"Aurora\",\"canChangePassword\":true}");
	}

	@Test
	void namesTheAdminKeysLoginAndSaysItCannotChangeAPassword() {
		String login = valueOf(logIn("ADMIN", FixedRandom.ADMIN_KEY_OF_ONES).getHeaders().getFirst(SET_COOKIE));

		HttpResponse<String> response = Unirest.get("/api/v1/me").header("Cookie", "epc_session=" + login).asString();

		assertThat(response.getBody()).isEqualTo("{\"name\":\"admin\",\"canChangePassword\":false}");
	}

	@Test
	void namesNoOneWithoutALogin() {
		int status = new RawHttpClient(server.getPort()).statusOfRequest("GET /api/v1/me HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n");

		assertThat(status).isEqualTo(UNAUTHORIZED.getCode());
	}

	@Test
	void clearsTheCookieOfALoginThatLogsOut() {
		String login = valueOf(logIn("Aurora", CHOSEN_PASSWORD).getHeaders().getFirst(SET_COOKIE));

		HttpResponse<String> response = Unirest.post("/api/v1/logout").header("Cookie", "epc_session=" + login).asString();

		assertThat(response.getStatus()).isEqualTo(NO_CONTENT.getCode());
		assertThat(response.getHeaders().getFirst(SET_COOKIE)).isEqualTo("epc_session=; Max-Age=0; Path=/; HttpOnly; SameSite=Lax");
	}

	@Test
	void opensNothingMoreWithTheLoginThatLoggedOut() {
		String login = valueOf(logIn("Aurora", CHOSEN_PASSWORD).getHeaders().getFirst(SET_COOKIE));
		Unirest.post("/api/v1/logout").header("Cookie", "epc_session=" + login).asString();

		HttpResponse<String> response = Unirest.get(PROTECTED_PATH).header("Cookie", "epc_session=" + login).asString();

		assertThat(response.getStatus()).isEqualTo(UNAUTHORIZED.getCode());
	}

	@Test
	void keepsAnotherLoginOfTheSameAccountOpenWhenOneLogsOut() {
		String first = valueOf(logIn("Aurora", CHOSEN_PASSWORD).getHeaders().getFirst(SET_COOKIE));
		String second = valueOf(logIn("Aurora", CHOSEN_PASSWORD).getHeaders().getFirst(SET_COOKIE));
		Unirest.post("/api/v1/logout").header("Cookie", "epc_session=" + first).asString();

		HttpResponse<String> response = Unirest.get(PROTECTED_PATH).header("Cookie", "epc_session=" + second).asString();

		assertThat(response.getStatus()).isEqualTo(OK.getCode());
	}

	@Test
	void refusesALogoutWithoutALogin() {
		int status = new RawHttpClient(server.getPort()).statusOfRequest("POST /api/v1/logout HTTP/1.1\r\nHost: localhost\r\nContent-Length: 0\r\nConnection: close\r\n\r\n");

		assertThat(status).isEqualTo(UNAUTHORIZED.getCode());
	}

	@Test
	void changesThePasswordOfTheLoginAndAnswersWithAFreshLogin() {
		holdAccount("Greta", CHOSEN_PASSWORD, PasswordState.CHOSEN, Role.MEMBER);
		String login = valueOf(logIn("Greta", CHOSEN_PASSWORD).getHeaders().getFirst(SET_COOKIE));

		HttpResponse<String> response = changePassword(login, CHOSEN_PASSWORD, "Winter-2026!");

		assertThat(response.getStatus()).isEqualTo(NO_CONTENT.getCode());
		assertThat(response.getHeaders().getFirst(SET_COOKIE)).matches(SESSION_COOKIE);
		assertThat(logIn("Greta", "Winter-2026!").getBody()).isEqualTo("{\"step\":\"logged-in\",\"next\":\"/\"}");
		assertThat(logIn("Greta", CHOSEN_PASSWORD).getStatus()).isEqualTo(UNAUTHORIZED.getCode());
	}

	@Test
	void keepsTheLoginTheChangeAnsweredWithAndEndsTheOtherLoginsOfTheAccount() {
		holdAccount("Hanna", CHOSEN_PASSWORD, PasswordState.CHOSEN, Role.MEMBER);
		String first = valueOf(logIn("Hanna", CHOSEN_PASSWORD).getHeaders().getFirst(SET_COOKIE));
		String second = valueOf(logIn("Hanna", CHOSEN_PASSWORD).getHeaders().getFirst(SET_COOKIE));

		String fresh = valueOf(changePassword(first, CHOSEN_PASSWORD, "Winter-2026!").getHeaders().getFirst(SET_COOKIE));

		assertThat(statusOfProtectedPathWith(fresh)).isEqualTo(OK.getCode());
		assertThat(statusOfProtectedPathWith(first)).isEqualTo(UNAUTHORIZED.getCode());
		assertThat(statusOfProtectedPathWith(second)).isEqualTo(UNAUTHORIZED.getCode());
	}

	@Test
	void refusesAWrongCurrentPasswordWith400AndChangesNothing() {
		holdAccount("Ida", CHOSEN_PASSWORD, PasswordState.CHOSEN, Role.MEMBER);
		String login = valueOf(logIn("Ida", CHOSEN_PASSWORD).getHeaders().getFirst(SET_COOKIE));

		HttpResponse<String> response = changePassword(login, "Herbst-2026!", "Winter-2026!");

		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST.getCode());
		assertThat(response.getBody()).isEqualTo("{\"refusal\":\"wrong-current-password\"}");
		assertThat(response.getHeaders().containsKey(SET_COOKIE)).isFalse();
		assertThat(logIn("Ida", CHOSEN_PASSWORD).getStatus()).isEqualTo(OK.getCode());
	}

	@Test
	void refusesAMissingCurrentPasswordAsAWrongOne() {
		holdAccount("Jana", CHOSEN_PASSWORD, PasswordState.CHOSEN, Role.MEMBER);
		String login = valueOf(logIn("Jana", CHOSEN_PASSWORD).getHeaders().getFirst(SET_COOKIE));

		HttpResponse<String> response = postPassword(login, "{\"newPassword\":\"Winter-2026!\"}");

		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST.getCode());
		assertThat(response.getBody()).isEqualTo("{\"refusal\":\"wrong-current-password\"}");
	}

	@Test
	void refusesANewPasswordThatBreaksTheRuleOrIsTheCurrentOneWith400() {
		holdAccount("Karla", CHOSEN_PASSWORD, PasswordState.CHOSEN, Role.MEMBER);
		String login = valueOf(logIn("Karla", CHOSEN_PASSWORD).getHeaders().getFirst(SET_COOKIE));

		HttpResponse<String> breaksTheRule = changePassword(login, CHOSEN_PASSWORD, "Abc-123");
		HttpResponse<String> sameAsBefore = changePassword(login, CHOSEN_PASSWORD, CHOSEN_PASSWORD);

		assertThat(List.of(breaksTheRule.getStatus(), sameAsBefore.getStatus())).containsOnly(BAD_REQUEST.getCode());
		assertThat(breaksTheRule.getBody()).isEqualTo("{\"refusal\":\"breaks-the-rule\"}");
		assertThat(sameAsBefore.getBody()).isEqualTo("{\"refusal\":\"same-as-before\"}");
	}

	@Test
	void refusesTheLoginOfTheAdminKeyWith403ForLackingAnAccount() {
		String login = valueOf(logIn("admin", FixedRandom.ADMIN_KEY_OF_ONES).getHeaders().getFirst(SET_COOKIE));

		HttpResponse<String> response = changePassword(login, FixedRandom.ADMIN_KEY_OF_ONES, "Winter-2026!");

		assertThat(response.getStatus()).isEqualTo(FORBIDDEN.getCode());
		assertThat(response.getBody()).isEqualTo("{\"refusal\":\"no-account\"}");
	}

	@Test
	void refusesAChangeWithoutALogin() {
		String body = "{\"currentPassword\":\"" + CHOSEN_PASSWORD + "\",\"newPassword\":\"Winter-2026!\"}";
		String request = "POST /api/v1/password HTTP/1.1\r\nHost: localhost\r\nContent-Type: application/json\r\nContent-Length: " + body.length() + "\r\nConnection: close\r\n\r\n"
				+ body;

		int status = new RawHttpClient(server.getPort()).statusOfRequest(request);

		assertThat(status).isEqualTo(UNAUTHORIZED.getCode());
	}

	@Test
	void refusesABodyOverFourKibibytesForAChangeWith413() {
		String login = valueOf(logIn("Aurora", CHOSEN_PASSWORD).getHeaders().getFirst(SET_COOKIE));

		HttpResponse<String> response = changePassword(login, CHOSEN_PASSWORD, "x".repeat(FIVE_KIBIBYTES));

		assertThat(response.getStatus()).isEqualTo(REQUEST_ENTITY_TOO_LARGE.getCode());
	}

	private HttpResponse<String> changePassword(String login, String currentPassword, String newPassword) {
		return postPassword(login, new JSONObject(Map.of("currentPassword", currentPassword, "newPassword", newPassword)).toString());
	}

	private static HttpResponse<String> postPassword(String login, String body) {
		return Unirest.post("/api/v1/password").header("Cookie", "epc_session=" + login).header("Content-Type", "application/json").body(body).asString();
	}

	private static int statusOfProtectedPathWith(String login) {
		return Unirest.get(PROTECTED_PATH).header("Cookie", "epc_session=" + login).asString().getStatus();
	}

	private HttpResponse<String> logIn(String name, String password, String next) {
		return post("/api/v1/login", new JSONObject(Map.of("name", name, "password", password, "next", next)).toString());
	}

	private HttpResponse<String> logIn(String name, String password) {
		return post("/api/v1/login", new JSONObject(Map.of("name", name, "password", password)).toString());
	}

	private HttpResponse<String> chooseNewPassword(String name, String issuedPassword, String newPassword) {
		return post("/api/v1/login/new-password", new JSONObject(Map.of("name", name, "issuedPassword", issuedPassword, "newPassword", newPassword)).toString());
	}

	private static HttpResponse<String> post(String body) {
		return post("/api/v1/login", body);
	}

	private static HttpResponse<String> post(String path, String body) {
		return Unirest.post(path).header("Content-Type", "application/json").body(body).asString();
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
