package dev.schoenberg.evergore.protocolParser.rest.exceptionHandler;

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
import static org.assertj.core.api.Assertions.assertThat;

@MicronautTest
class ApplicationExceptionHandlerHttpTest {
	private static final Path WORKING_DB = Paths.get("build/tmp/exceptionHandlerHttp/exceptionHandlerHttp.sqlite");
	private static final String UNKNOWN_AVATAR = "definitely-unknown";
	private static final String UNKNOWN_AVATAR_WITH_CONTROL_CHARACTERS = "unknown%0D%0AInjected:%20header";

	static {
		silentThrow(() -> {
			Files.createDirectories(WORKING_DB.getParent());
			Files.deleteIfExists(WORKING_DB);
			try (InputStream src = ApplicationExceptionHandlerHttpTest.class.getResourceAsStream("/testdata.sqlite")) {
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
	void theNotFoundReasonPhraseDoesNotEchoTheRequestedAvatar() {
		HttpResponse<String> response = requestBankPageOf(UNKNOWN_AVATAR);

		assertThat(response.getStatusText()).isEqualTo("Not Found");
	}

	@Test
	void unknownAvatarCarryingControlCharactersAnswersNotFoundInsteadOfAServerError() {
		HttpResponse<String> response = requestBankPageOf(UNKNOWN_AVATAR_WITH_CONTROL_CHARACTERS);

		assertThat(response.getStatus()).isEqualTo(404);
	}

	private HttpResponse<String> requestBankPageOf(String avatar) {
		return Unirest.get("/api/v1/avatars/" + avatar + "/bank?token=test-token").asString();
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
