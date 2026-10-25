package dev.schoenberg.evergore.protocolParser;

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

import dev.schoenberg.evergore.protocolParser.application.ChangePassword;
import dev.schoenberg.evergore.protocolParser.application.ChangePasswordOutcome;
import dev.schoenberg.evergore.protocolParser.application.EvergoreDataExtractor;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.Login;
import dev.schoenberg.evergore.protocolParser.dataExtraction.PostCollectionHook;
import dev.schoenberg.evergore.protocolParser.database.PreDatabaseConnectionHook;
import dev.schoenberg.evergore.protocolParser.helper.config.Configuration;

import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.silentThrow;
import static io.micronaut.http.HttpStatus.UNAUTHORIZED;
import static org.assertj.core.api.Assertions.assertThat;

@MicronautTest
class PasswordEndpointEndedLoginTest {
	private static final Path WORKING_DB = Paths.get("build/tmp/passwordEndpointEndedLogin/passwordEndpointEndedLogin.sqlite");
	private static final String CHANGE_BODY = "{\"currentPassword\":\"Sommer-2026!\",\"newPassword\":\"Winter-2026!\"}";

	static {
		silentThrow(() -> {
			Files.createDirectories(WORKING_DB.getParent());
			Files.deleteIfExists(WORKING_DB);
		});
	}

	private @Inject EmbeddedServer server;

	@BeforeEach
	void setup() {
		Unirest.config().verifySsl(false);
		Unirest.config().defaultBaseUrl("http://localhost:" + server.getPort());
	}

	@Test
	void answers401WithoutACookieWhenAnotherChangeEndedTheLoginWhileThisOneWasChecked() {
		String cookie = LoginCookie.memberHeaderFor(server, "Aurora");

		HttpResponse<String> response = Unirest.post("/api/v1/password").header("Cookie", cookie).header("Content-Type", "application/json").body(CHANGE_BODY).asString();

		assertThat(response.getStatus()).isEqualTo(UNAUTHORIZED.getCode());
		assertThat(response.getHeaders().containsKey("Set-Cookie")).isFalse();
	}

	@MockBean(ChangePassword.class)
	ChangePassword changePasswordThatFindsTheLoginEnded() {
		return new ChangePassword(null, null, null, null, null, null) {
			@Override
			public ChangePasswordOutcome with(Login login, String currentPassword, String newPassword) {
				return new ChangePasswordOutcome.LoginEnded();
			}
		};
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
