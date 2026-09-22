package dev.schoenberg.evergore.protocolParser;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.schoenberg.evergore.protocolParser.database.metaInformation.MetaInformationDatabaseRepository;

import static dev.schoenberg.evergore.protocolParser.ThrowawayDatabaseFactory.THROWAWAY_DATABASE_PATH;
import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.silentThrow;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class ContextShutdownClosesTheDatabaseTest {
	private static final String THROWAWAY_DB_PATH = "build/tmp/test/contextShutdownClosesTheDatabaseTest.sqlite";

	@BeforeEach
	void deleteTheThrowawayDatabase() {
		silentThrow(() -> Files.deleteIfExists(Paths.get(THROWAWAY_DB_PATH)));
	}

	@Test
	void theMetaRepositoryCanNoLongerReadOnceItsContextIsClosed() {
		MetaInformationDatabaseRepository tested = metaRepositoryOfAClosedContext();

		Throwable failure = catchThrowable(tested::snapshot);

		assertThat(failure).isNotNull();
	}

	private static MetaInformationDatabaseRepository metaRepositoryOfAClosedContext() {
		try (ApplicationContext context = ApplicationContext.run(Map.of(THROWAWAY_DATABASE_PATH, THROWAWAY_DB_PATH), "test")) {
			return context.getBean(MetaInformationDatabaseRepository.class);
		}
	}
}
