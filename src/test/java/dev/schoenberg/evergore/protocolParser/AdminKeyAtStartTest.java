package dev.schoenberg.evergore.protocolParser;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static dev.schoenberg.evergore.protocolParser.ThrowawayDatabaseFactory.THROWAWAY_DATABASE_PATH;
import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.silentThrow;
import static org.assertj.core.api.Assertions.assertThat;

class AdminKeyAtStartTest {
	private static final String DATABASE = "build/tmp/test/adminKeyAtStart.sqlite";
	private static final String LINE = "Console admin key, valid until the next restart: ";

	@BeforeEach
	void deleteTheThrowawayDatabase() {
		silentThrow(() -> Files.deleteIfExists(Paths.get(DATABASE)));
	}

	@Test
	void logsOneAdminKeyAtTheStartBeforeAnythingAsksForIt() {
		try (ApplicationContext context = ApplicationContext.run(Map.of(THROWAWAY_DATABASE_PATH, DATABASE, SpiedLoggerFactory.SPIED_LOGGER, "true"), "test")) {
			List<String> keyLines = context.getBean(LoggerSpy.class).infoMessages().stream().filter(line -> line.startsWith(LINE)).toList();

			assertThat(keyLines).singleElement().asString().matches(LINE + "[A-Za-z0-9_-]{43}");
		}
	}
}
