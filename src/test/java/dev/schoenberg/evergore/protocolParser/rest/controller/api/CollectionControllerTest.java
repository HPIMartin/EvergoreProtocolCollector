package dev.schoenberg.evergore.protocolParser.rest.controller.api;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;

import jakarta.inject.Inject;

import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.schoenberg.evergore.protocolParser.LoginCookie;
import dev.schoenberg.evergore.protocolParser.application.Collections;
import dev.schoenberg.evergore.protocolParser.application.EvergoreDataExtractor;
import dev.schoenberg.evergore.protocolParser.application.StartOutcome;
import dev.schoenberg.evergore.protocolParser.dataExtraction.PostCollectionHook;
import dev.schoenberg.evergore.protocolParser.database.PreDatabaseConnectionHook;
import dev.schoenberg.evergore.protocolParser.helper.config.Configuration;

import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.silentThrow;
import static org.assertj.core.api.Assertions.assertThat;

@MicronautTest
class CollectionControllerTest {
	private static final Path WORKING_DB = Paths.get("build/tmp/collectionController/collectionController.sqlite");
	private static final String PATH = "/api/v1/admin/collection";

	static {
		silentThrow(() -> {
			Files.createDirectories(WORKING_DB.getParent());
			Files.deleteIfExists(WORKING_DB);
			try (InputStream src = CollectionControllerTest.class.getResourceAsStream("/testdata.sqlite")) {
				Files.copy(src, WORKING_DB);
			}
		});
	}

	private @Inject EmbeddedServer server;
	private @Inject Collections collections;

	@BeforeEach
	void setup() {
		Unirest.config().verifySsl(false);
		Unirest.config().defaultBaseUrl("http://localhost:" + server.getPort());
		stub().answer = new StartOutcome.Started();
		stub().starts = 0;
	}

	@Test
	void acceptsAStartWith202AndStartsOneCollection() {
		HttpResponse<String> response = Unirest.post(PATH).header("Cookie", LoginCookie.headerFor(server)).asString();

		assertThat(response.getStatus()).isEqualTo(202);
		assertThat(stub().starts).isEqualTo(1);
	}

	@Test
	void startsACollectionOnlyForTheAdminRole() {
		HttpResponse<String> withoutALogin = Unirest.post(PATH).asString();
		HttpResponse<String> withAMembersLogin = Unirest.post(PATH).header("Cookie", LoginCookie.memberHeaderFor(server, "Aurora")).asString();
		int startsAfterTheRefusals = stub().starts;

		HttpResponse<String> withTheAdminsLogin = Unirest.post(PATH).header("Cookie", LoginCookie.headerFor(server)).asString();

		assertThat(withoutALogin.getStatus()).isEqualTo(401);
		assertThat(withAMembersLogin.getStatus()).isEqualTo(403);
		assertThat(startsAfterTheRefusals).isZero();
		assertThat(withTheAdminsLogin.getStatus()).isEqualTo(202);
		assertThat(stub().starts).isEqualTo(1);
	}

	@Test
	void refusesAStartWhileACollectionRunsWith409AndTheReasonRunning() {
		stub().answer = new StartOutcome.AlreadyRunning(Instant.parse("2026-01-02T12:00:00Z"));

		HttpResponse<String> response = Unirest.post(PATH).header("Cookie", LoginCookie.headerFor(server)).asString();

		assertThat(response.getStatus()).isEqualTo(409);
		assertThat(new JSONObject(response.getBody()).getString("refusal")).isEqualTo("running");
	}

	@Test
	void refusesAStartWithinTheHourOfTheLastStartWith409AndTheReasonTooEarly() {
		stub().answer = new StartOutcome.TooEarly(Instant.parse("2026-01-02T13:00:00Z"));

		HttpResponse<String> response = Unirest.post(PATH).header("Cookie", LoginCookie.headerFor(server)).asString();

		assertThat(response.getStatus()).isEqualTo(409);
		assertThat(new JSONObject(response.getBody()).getString("refusal")).isEqualTo("too-early");
	}

	private StubbedCollections stub() {
		return (StubbedCollections) collections;
	}

	@MockBean(Collections.class)
	StubbedCollections stubbedCollections() {
		return new StubbedCollections();
	}

	public static class StubbedCollections extends Collections {
		StartOutcome answer = new StartOutcome.Started();
		int starts;

		public StubbedCollections() {
			super(null, null, null, null);
		}

		@Override
		public StartOutcome startByHand() {
			starts++;
			return answer;
		}
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
