package dev.schoenberg.evergore.protocolParser.helper.security;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.schoenberg.evergore.protocolParser.application.SignedLogins;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.Login;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.Role;

import static dev.schoenberg.evergore.protocolParser.ThrowawayDatabaseFactory.THROWAWAY_DATABASE_PATH;
import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.silentThrow;
import static org.assertj.core.api.Assertions.assertThat;

class SignedLoginsBeanTest {
	private static final String FIRST_START_DB = "build/tmp/test/signedLoginsFirstStart.sqlite";
	private static final String SECOND_START_DB = "build/tmp/test/signedLoginsSecondStart.sqlite";

	@BeforeEach
	void deleteTheThrowawayDatabases() {
		silentThrow(() -> Files.deleteIfExists(Paths.get(FIRST_START_DB)));
		silentThrow(() -> Files.deleteIfExists(Paths.get(SECOND_START_DB)));
	}

	@Test
	void theServiceReadsTheLoginItIssued() {
		try (ApplicationContext context = started(FIRST_START_DB)) {
			SignedLogins tested = context.getBean(SignedLogins.class);
			String value = tested.issue("aurora", Role.MEMBER, 0);

			Optional<Login> login = tested.read(value);

			assertThat(login).map(Login::accountName).contains("aurora");
		}
	}

	@Test
	void aLoginIssuedBeforeARestartIsNoLoginAfterIt() {
		String beforeTheRestart = loginIssuedAtAStartOn(FIRST_START_DB);

		Optional<Login> login = readAtAStartOn(SECOND_START_DB, beforeTheRestart);

		assertThat(login).isEmpty();
	}

	private static String loginIssuedAtAStartOn(String database) {
		try (ApplicationContext context = started(database)) {
			return context.getBean(SignedLogins.class).issue("aurora", Role.ADMIN, 0);
		}
	}

	private static Optional<Login> readAtAStartOn(String database, String value) {
		try (ApplicationContext context = started(database)) {
			return context.getBean(SignedLogins.class).read(value);
		}
	}

	private static ApplicationContext started(String database) {
		return ApplicationContext.run(Map.of(THROWAWAY_DATABASE_PATH, database), "test");
	}
}
