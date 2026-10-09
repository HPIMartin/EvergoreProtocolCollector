package dev.schoenberg.evergore.protocolParser.rest.filter;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import jakarta.inject.Inject;

import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

import dev.schoenberg.evergore.protocolParser.Logger;
import dev.schoenberg.evergore.protocolParser.LoggerSpy;
import dev.schoenberg.evergore.protocolParser.RawHttpClient;
import dev.schoenberg.evergore.protocolParser.application.EvergoreDataExtractor;
import dev.schoenberg.evergore.protocolParser.dataExtraction.PostCollectionHook;
import dev.schoenberg.evergore.protocolParser.database.PreDatabaseConnectionHook;
import dev.schoenberg.evergore.protocolParser.helper.config.Configuration;

import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.silentThrow;
import static io.micronaut.http.HttpStatus.OK;
import static io.micronaut.http.HttpStatus.REQUEST_ENTITY_TOO_LARGE;
import static io.micronaut.http.HttpStatus.TOO_MANY_REQUESTS;
import static org.assertj.core.api.Assertions.assertThat;

@MicronautTest(environments = "ratelimit", rebuildContext = true)
class UnreadableHeadServerTest {

	private static final Path WORKING_DB = Paths.get("build/tmp/unreadableHead/unreadableHead.sqlite");
	private static final Pattern STATUS_LINE = Pattern.compile("HTTP/1\\.1 \\d{3}");
	private static final String AUDIT_LINE_START = "Client IP: ";
	private static final int FRAME_SIZE = 1460;
	private static final String OVERSIZED_TARGET = "a".repeat(5000);
	private static final String BROWSER = "Mozilla/5.0 (X11; Linux x86_64; rv:128.0) Gecko/20100101 Firefox/128.0";

	static {
		silentThrow(() -> {
			Files.createDirectories(WORKING_DB.getParent());
			Files.deleteIfExists(WORKING_DB);
			try (InputStream src = UnreadableHeadServerTest.class.getResourceAsStream("/testdata.sqlite")) {
				Files.copy(src, WORKING_DB);
			}
		});
	}

	private final LoggerSpy logger = new LoggerSpy();

	private @Inject EmbeddedServer server;

	@Test
	void answersARejectedHeadOnceAndNeitherAnswersNorCountsTheRequestPipelinedBehindIt() {
		RawHttpClient rawClient = new RawHttpClient(server.getPort());
		String rejectedHead = "GET /" + OVERSIZED_TARGET + " HTTP/1.1\r\nHost: localhost\r\nUser-Agent: first\r\n\r\n";
		String pipelined = "GET /favicon.ico HTTP/1.1\r\nHost: localhost\r\n\r\n";

		String answer = rawClient.answerUntilTheServerCloses(rejectedHead + pipelined);
		List<String> auditedAfterwards = auditLines();
		int statusOfALaterRequest = rawClient.statusOf("/favicon.ico");

		assertThat(answer).startsWith("HTTP/1.1 " + REQUEST_ENTITY_TOO_LARGE.getCode());
		assertThat(answer).containsIgnoringCase("connection: close");
		assertThat(STATUS_LINE.matcher(answer).results()).hasSize(1);
		assertThat(auditedAfterwards).hasSize(1);
		assertThat(statusOfALaterRequest).isEqualTo(OK.getCode());
	}

	@Test
	void answersAndCountsOnceAnIncompleteHeadWhoseRestExceedsTheHeaderBudgetAndClosesTheConnection() {
		RawHttpClient rawClient = new RawHttpClient(server.getPort());

		String answer = rawClient.answerUntilTheServerCloses(overBudgetHeadWithoutItsBlankLine());

		assertThat(answer).startsWith("HTTP/1.1 " + REQUEST_ENTITY_TOO_LARGE.getCode());
		assertThat(STATUS_LINE.matcher(answer).results()).hasSize(1);
		assertThat(auditLines()).containsExactly(AUDIT_LINE_START + "127.0.0.1 Agent: " + BROWSER);
	}

	@Test
	void countsAnIncompleteHeadWhoseRestExceedsTheHeaderBudgetAgainstTheRateLimit() {
		RawHttpClient rawClient = new RawHttpClient(server.getPort());
		String head = overBudgetHeadWithoutItsBlankLine();

		List<String> answers = IntStream.range(0, 2).mapToObj(_ -> rawClient.answerUntilTheServerCloses(head)).toList();
		int statusOfALaterRequest = rawClient.statusOf("/favicon.ico");

		assertThat(answers).allMatch(answer -> answer.startsWith("HTTP/1.1 " + REQUEST_ENTITY_TOO_LARGE.getCode()));
		assertThat(statusOfALaterRequest).isEqualTo(TOO_MANY_REQUESTS.getCode());
	}

	@Test
	void neitherAnswersNorCountsAPlainHeadCutOffByTheClientClosingTheConnection() {
		RawHttpClient rawClient = new RawHttpClient(server.getPort());
		String cutOffHead = "GET / HTTP/1.1\r\nHost: localhost\r\n";

		String answer = rawClient.answerToHalfClosedRequest(cutOffHead);
		List<Integer> statuses = IntStream.range(0, 2).mapToObj(_ -> rawClient.statusOf("/favicon.ico")).toList();

		assertThat(answer).isEmpty();
		assertThat(statuses).containsExactly(OK.getCode(), OK.getCode());
	}

	@Test
	void logsOneLineWithTheClientAndTheBrowserOfARejectedHeadSentInPiecesAndNeverItsQuery() {
		RawHttpClient rawClient = new RawHttpClient(server.getPort());
		String browserHead = "GET /overview?token=SECRET" + OVERSIZED_TARGET + " HTTP/1.1\r\nHost: localhost\r\nUser-Agent: " + BROWSER
				+ "\r\nAccept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8\r\nAccept-Language: en-US,en;q=0.5\r\nAccept-Encoding: gzip, deflate\r\n"
				+ "Connection: keep-alive\r\nUpgrade-Insecure-Requests: 1\r\n\r\n";

		int status = rawClient.statusOfRequestSentInPieces(browserHead, FRAME_SIZE);

		assertThat(status).isEqualTo(REQUEST_ENTITY_TOO_LARGE.getCode());
		assertThat(auditLines()).containsExactly(AUDIT_LINE_START + "127.0.0.1 Agent: " + BROWSER);
		assertThat(Stream.of(logger.infoMessages(), logger.warnMessages(), logger.errorMessages()).flatMap(List::stream)).noneMatch(line -> line.contains("token="));
	}

	private static String overBudgetHeadWithoutItsBlankLine() {
		String padding = IntStream.range(0, 100).mapToObj(number -> "X-Padding-" + number + ": " + "p".repeat(100) + "\r\n").collect(Collectors.joining());
		return "GET /" + OVERSIZED_TARGET + " HTTP/1.1\r\nHost: localhost\r\nUser-Agent: " + BROWSER + "\r\n" + padding;
	}

	private List<String> auditLines() {
		return logger.infoMessages().stream().filter(line -> line.startsWith(AUDIT_LINE_START)).toList();
	}

	@MockBean(Logger.class)
	Logger loggerSpy() {
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
