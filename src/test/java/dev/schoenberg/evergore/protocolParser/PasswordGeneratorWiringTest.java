package dev.schoenberg.evergore.protocolParser;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.schoenberg.evergore.protocolParser.application.AccountRepository;
import dev.schoenberg.evergore.protocolParser.application.PasswordGenerator;
import dev.schoenberg.evergore.protocolParser.application.ResetPassword;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.Account;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.PasswordState;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.Role;

import static dev.schoenberg.evergore.protocolParser.ThrowawayDatabaseFactory.THROWAWAY_DATABASE_PATH;
import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.silentThrow;
import static org.assertj.core.api.Assertions.assertThat;

class PasswordGeneratorWiringTest {
	private static final String DATABASE = "build/tmp/test/passwordGeneratorWiring.sqlite";
	private static final String GENERATED = "Generated-by-the-context-1!";

	@BeforeEach
	void deleteTheThrowawayDatabase() {
		silentThrow(() -> Files.deleteIfExists(Paths.get(DATABASE)));
	}

	@Test
	void issuesTheResetPasswordFromThePasswordGeneratorOfTheContext() {
		PasswordGenerator generatorOfTheContext = () -> GENERATED;
		try (ApplicationContext context = ApplicationContext.builder("test").properties(Map.of(THROWAWAY_DATABASE_PATH, DATABASE)).build()) {
			context.registerSingleton(PasswordGenerator.class, generatorOfTheContext);
			context.start();
			context.getBean(AccountRepository.class).addIfAbsent(new Account("Aurora", "hash", PasswordState.CHOSEN, Role.MEMBER, Optional.empty()));

			Optional<String> issued = context.getBean(ResetPassword.class).forAccount("Aurora");

			assertThat(issued).hasValue(GENERATED);
		}
	}
}
