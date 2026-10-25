package dev.schoenberg.evergore.protocolParser.helper.config;

import java.util.Map;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import static dev.schoenberg.evergore.protocolParser.ThrowawayDatabaseFactory.THROWAWAY_DATABASE_PATH;
import static org.assertj.core.api.Assertions.assertThat;

class PasswordHashConfigurationTest {
	private static final String THROWAWAY_DB_PATH = "build/tmp/test/passwordHashConfigurationTest.sqlite";

	@Test
	void defaultsToSixtyFourMebibytesThreeIterationsOneLaneAndTwoConcurrentChecks() {
		PasswordHashConfiguration tested = configurationOfTheProductionDefaults();

		assertThat(tested).isEqualTo(new PasswordHashConfiguration(65536, 3, 1, 2));
	}

	@Test
	void readsTheValuesTheTestProfileSetsInsteadOfTheDefaults() {
		try (ApplicationContext context = ApplicationContext.run(Map.of(THROWAWAY_DATABASE_PATH, THROWAWAY_DB_PATH), "test")) {
			PasswordHashConfiguration tested = context.getBean(PasswordHashConfiguration.class);

			assertThat(tested).isEqualTo(new PasswordHashConfiguration(1024, 1, 1, 2));
		}
	}

	private static PasswordHashConfiguration configurationOfTheProductionDefaults() {
		Map<String, Object> required = Map
				.of("evergore.credentials.username", "user", "evergore.credentials.password", "secret", "micronaut.server.port", "-1", THROWAWAY_DATABASE_PATH, THROWAWAY_DB_PATH);

		try (ApplicationContext context = ApplicationContext.builder().deduceEnvironment(false).environmentPropertySource(false).properties(required).start()) {
			return context.getBean(PasswordHashConfiguration.class);
		}
	}
}
