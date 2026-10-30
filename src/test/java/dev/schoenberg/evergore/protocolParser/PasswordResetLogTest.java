package dev.schoenberg.evergore.protocolParser;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.micronaut.context.ApplicationContext;
import io.micronaut.runtime.server.EmbeddedServer;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.schoenberg.evergore.protocolParser.application.AccountRepository;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.Account;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.PasswordState;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.Role;

import static dev.schoenberg.evergore.protocolParser.ThrowawayDatabaseFactory.THROWAWAY_DATABASE_PATH;
import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.silentThrow;
import static org.assertj.core.api.Assertions.assertThat;

class PasswordResetLogTest {
	private static final String DATABASE = "build/tmp/test/passwordResetLog.sqlite";

	@BeforeEach
	void deleteTheThrowawayDatabase() {
		silentThrow(() -> Files.deleteIfExists(Paths.get(DATABASE)));
	}

	@Test
	void keepsTheIssuedPasswordOutOfTheServiceLogAndOutOfEveryAnswerButTheResetsOwn() {
		Map<String, Object> properties = Map.of(THROWAWAY_DATABASE_PATH, DATABASE, SpiedLoggerFactory.SPIED_LOGGER, "true", "micronaut.server.port", "-1");
		try (EmbeddedServer server = ApplicationContext.run(EmbeddedServer.class, properties, "test")) {
			server.getApplicationContext().getBean(AccountRepository.class).addIfAbsent(new Account("Vergesslich", "hash", PasswordState.CHOSEN, Role.MEMBER, Optional.empty()));
			Unirest.config().defaultBaseUrl("http://localhost:" + server.getPort());
			String admin = LoginCookie.headerFor(server);

			HttpResponse<String> reset = Unirest.post("/api/v1/admin/accounts/Vergesslich/reset").header("Cookie", admin).asString();
			String issued = new JSONObject(reset.getBody()).getString("password");
			HttpResponse<String> accountList = Unirest.get("/api/v1/admin/accounts").header("Cookie", admin).asString();
			HttpResponse<String> adminStatus = Unirest.get("/api/v1/admin/status").header("Cookie", admin).asString();

			LoggerSpy logger = server.getApplicationContext().getBean(LoggerSpy.class);
			List<String> logged = new ArrayList<>(logger.infoMessages());
			logged.addAll(logger.warnMessages());
			logged.addAll(logger.errorMessages());
			assertThat(issued).isNotBlank();
			assertThat(logged).isNotEmpty().noneMatch(line -> line.contains(issued));
			assertThat(accountList.getBody()).doesNotContain(issued);
			assertThat(adminStatus.getBody()).doesNotContain(issued);
		}
	}
}
