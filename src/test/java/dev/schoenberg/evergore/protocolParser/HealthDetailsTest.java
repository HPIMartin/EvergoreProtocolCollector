package dev.schoenberg.evergore.protocolParser;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import jakarta.inject.Inject;

import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.scheduling.DefaultTaskExceptionHandler;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.schoenberg.evergore.protocolParser.application.EvergoreDataExtractor;
import dev.schoenberg.evergore.protocolParser.application.SignedLogins;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.Role;
import dev.schoenberg.evergore.protocolParser.dataExtraction.PostCollectionHook;
import dev.schoenberg.evergore.protocolParser.database.PreDatabaseConnectionHook;
import dev.schoenberg.evergore.protocolParser.helper.config.Configuration;

import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.silentThrow;
import static org.assertj.core.api.Assertions.assertThat;

@MicronautTest
class HealthDetailsTest {
	private static final Path WORKING_DB = Paths.get("build/tmp/healthDetails/health.sqlite");
	private static final String DETAILS = "details";
	private static final String VERDICT_ALONE = "{\"status\":\"UP\"}";

	static {
		silentThrow(() -> {
			Files.createDirectories(WORKING_DB.getParent());
			Files.deleteIfExists(WORKING_DB);
			try (InputStream src = HealthDetailsTest.class.getResourceAsStream("/testdata.sqlite")) {
				Files.copy(src, WORKING_DB);
			}
		});
	}

	private @Inject EmbeddedServer server;
	private @Inject BootSignalRecorder signals;
	private @Inject SignedLogins signedLogins;

	@BeforeEach
	void setup() {
		Unirest.config().verifySsl(false);
		Unirest.config().defaultBaseUrl("http://localhost:" + server.getPort());
		signals.awaitCollection();
	}

	@Test
	void showsTheDetailsToTheAdminKeysLogin() {
		JSONObject health = healthWithCookie(LoginCookie.headerFor(server));

		assertThat(health.isNull(DETAILS)).isFalse();
	}

	@Test
	void showsTheDetailsToAnAccountWithTheAdminRole() {
		JSONObject health = healthWithCookie(LoginCookie.NAME + "=" + signedLogins.issue("Aurora", Role.ADMIN, 0));

		assertThat(health.isNull(DETAILS)).isFalse();
	}

	@Test
	void namesTheStatusBeforeTheDetailsToTheAdmin() {
		HttpResponse<String> response = Unirest.get("/health").header("Cookie", LoginCookie.headerFor(server)).asString();

		assertThat(response.getBody().indexOf("\"status\"")).isLessThan(response.getBody().indexOf("\"details\""));
	}

	@Test
	void showsTheVerdictAloneToAMembersLogin() {
		HttpResponse<String> response = Unirest.get("/health").header("Cookie", LoginCookie.NAME + "=" + signedLogins.issue("Aurora", Role.MEMBER, 0)).asString();

		assertThat(response.getBody()).isEqualTo(VERDICT_ALONE);
	}

	@Test
	void showsTheVerdictAloneToARequestWithoutALogin() {
		HttpResponse<String> response = Unirest.get("/health").asString();

		assertThat(response.getBody()).isEqualTo(VERDICT_ALONE);
	}

	@Test
	void showsTheVerdictAloneToALoginTheServiceDidNotIssue() {
		HttpResponse<String> response = Unirest.get("/health").header("Cookie", LoginCookie.NAME + "=forged").asString();

		assertThat(response.getBody()).isEqualTo(VERDICT_ALONE);
	}

	private static JSONObject healthWithCookie(String cookie) {
		return new JSONObject(Unirest.get("/health").header("Cookie", cookie).asString().getBody());
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
			return 0;
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
	PostCollectionHook collectionHook(BootSignalRecorder recorder) {
		return recorder::recordCollectionFinished;
	}

	@MockBean(DefaultTaskExceptionHandler.class)
	DefaultTaskExceptionHandler exceptionHandler(BootSignalRecorder recorder) {
		return new TestTaskExceptionHandler(recorder);
	}

	public static class TestTaskExceptionHandler extends DefaultTaskExceptionHandler {
		private final BootSignalRecorder signals;

		public TestTaskExceptionHandler(BootSignalRecorder signals) {
			this.signals = signals;
		}

		@Override
		public void handle(Object bean, Throwable throwable) {
			signals.recordException();
		}
	}
}
