package dev.schoenberg.evergore.protocolParser.helper.config;

import java.time.Duration;
import java.util.Map;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import static dev.schoenberg.evergore.protocolParser.ThrowawayDatabaseFactory.THROWAWAY_DATABASE_PATH;
import static org.assertj.core.api.Assertions.assertThat;

class UsageWritingConfigurationTest {
	private static final String THROWAWAY_DB_PATH = "build/tmp/test/usageWritingConfigurationTest.sqlite";

	@Test
	void defaultsToTenThousandWaitingRecordsBatchesOfFiveHundredAndACloseBoundOfTenSeconds() {
		Map<String, Object> required = Map
				.of("evergore.credentials.username", "user", "evergore.credentials.password", "secret", "micronaut.server.port", "-1", THROWAWAY_DATABASE_PATH, THROWAWAY_DB_PATH);

		try (ApplicationContext context = ApplicationContext.builder().deduceEnvironment(false).environmentPropertySource(false).properties(required).start()) {
			UsageWritingConfiguration tested = context.getBean(UsageWritingConfiguration.class);

			assertThat(tested).isEqualTo(new UsageWritingConfiguration(10_000, 500, Duration.ofSeconds(10)));
		}
	}
}
