package dev.schoenberg.evergore.protocolParser.rest.exceptionHandler;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import jakarta.inject.Inject;

import io.micronaut.context.annotation.Property;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.schoenberg.evergore.protocolParser.Logger;
import dev.schoenberg.evergore.protocolParser.LoggerSpy;
import dev.schoenberg.evergore.protocolParser.RawHttpClient;
import dev.schoenberg.evergore.protocolParser.application.EvergoreDataExtractor;
import dev.schoenberg.evergore.protocolParser.dataExtraction.PostCollectionHook;
import dev.schoenberg.evergore.protocolParser.database.PreDatabaseConnectionHook;
import dev.schoenberg.evergore.protocolParser.helper.config.Configuration;

import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.silentThrow;
import static org.assertj.core.api.Assertions.assertThat;

@MicronautTest
@Property(name = "evergore.security.public-paths", value = "/api/v1/login,/api/v1/boom,/api/v1/echo")
class BareErrorAnswersTest {
	private static final Path WORKING_DB = Paths.get("build/tmp/bareErrorAnswers/bareErrorAnswers.sqlite");
	private static final String BOOM_PATH = "/api/v1/boom";
	private static final String ECHO_PATH = "/api/v1/echo";
	private static final String LOGIN_PATH = "/api/v1/login";
	private static final String LEAKING_MESSAGE = "jdbc:sqlite:database/temp.sqlite opening db: 'database/temp.sqlite': Permission denied";
	private static final String NOT_JSON = "not-json-at-all";

	static {
		silentThrow(() -> {
			Files.createDirectories(WORKING_DB.getParent());
			Files.deleteIfExists(WORKING_DB);
			try (InputStream src = BareErrorAnswersTest.class.getResourceAsStream("/testdata.sqlite")) {
				Files.copy(src, WORKING_DB);
			}
		});
	}

	private final LoggerSpy logger = new LoggerSpy();

	private @Inject EmbeddedServer server;

	@BeforeEach
	void setup() {
		Unirest.config().verifySsl(false);
		Unirest.config().defaultBaseUrl("http://localhost:" + server.getPort());
	}

	@Test
	void answersAnUnhandledExceptionWithABareServerErrorThatNamesNothingInternal() {
		HttpResponse<String> response = Unirest.get(BOOM_PATH).asString();

		assertThat(response.getStatus()).isEqualTo(500);
		assertThat(response.getBody()).isNullOrEmpty();
	}

	@Test
	void logsTheStackTraceOfAnUnhandledExceptionAtErrorOnTheServer() {
		Unirest.get(BOOM_PATH).asString();

		assertThat(logger.errorThrowables()).anyMatch(error -> error.getMessage().contains(LEAKING_MESSAGE));
	}

	@Test
	void answersAMalformedLoginBodyWithABareBadRequestThatNamesNoParser() {
		HttpResponse<String> response = Unirest.post(LOGIN_PATH).header("Content-Type", "application/json").body(NOT_JSON).asString();

		assertThat(response.getStatus()).isEqualTo(400);
		assertThat(response.getBody()).isNullOrEmpty();
	}

	@Test
	void logsAMalformedBodyAsAWarningAndNotAsAnError() {
		Unirest.post(LOGIN_PATH).header("Content-Type", "application/json").body(NOT_JSON).asString();

		assertThat(logger.warnMessages()).anyMatch(message -> message.contains(LOGIN_PATH));
		assertThat(logger.errorMessages()).noneMatch(message -> message.contains(LOGIN_PATH));
	}

	@Test
	void answersABodyThatIsNoGzipUnderAGzipEncodingWithABadRequestAndLogsNoError() {
		int status = new RawHttpClient(server.getPort()).statusOfRequest(requestWithGarbageUnderGzip(ECHO_PATH));

		assertThat(status).isEqualTo(400);
		assertThat(logger.errorMessages()).noneMatch(message -> message.contains(ECHO_PATH));
	}

	@Test
	void turnsAwayAGzipEncodedLoginBodyWithoutLoggingAnError() {
		int status = new RawHttpClient(server.getPort()).statusOfRequest(requestWithGarbageUnderGzip(LOGIN_PATH));

		assertThat(status).isEqualTo(413);
		assertThat(logger.errorMessages()).noneMatch(message -> message.contains(LOGIN_PATH));
	}

	private static String requestWithGarbageUnderGzip(String path) {
		return "POST " + path
				+ " HTTP/1.1\r\nHost: localhost\r\nContent-Type: application/json\r\nTransfer-Encoding: gzip, chunked\r\nConnection: close\r\n\r\nd\r\nnot gzip data\r\n0\r\n\r\n";
	}

	@Controller("/api/v1")
	static class TestController {
		@Get("/boom")
		String boom() {
			throw new IllegalStateException(LEAKING_MESSAGE);
		}

		@Post("/echo")
		String echo(@Body String body) {
			return body;
		}
	}

	@MockBean(Logger.class)
	Logger loggerMock() {
		return logger;
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
