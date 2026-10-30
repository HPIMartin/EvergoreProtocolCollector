package dev.schoenberg.evergore.protocolParser.rest.controller.api;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.schoenberg.evergore.protocolParser.LoginCookie;
import dev.schoenberg.evergore.protocolParser.application.AccountRepository;
import dev.schoenberg.evergore.protocolParser.application.EvergoreDataExtractor;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.Account;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.PasswordState;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.Role;
import dev.schoenberg.evergore.protocolParser.businessLogic.banking.BankRepository;
import dev.schoenberg.evergore.protocolParser.dataExtraction.PostCollectionHook;
import dev.schoenberg.evergore.protocolParser.database.PreDatabaseConnectionHook;
import dev.schoenberg.evergore.protocolParser.helper.config.Configuration;

import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.silentThrow;
import static org.assertj.core.api.Assertions.assertThat;

@MicronautTest
class AccountsControllerTest {
	private static final Path WORKING_DB = Paths.get("build/tmp/accountsController/accountsController.sqlite");
	private static final Instant LOGGED_IN = Instant.parse("2026-01-05T10:00:00Z");
	private static final String SECRET_HASH = "hash-nobody-may-read";
	private static final String NEVER_MOVED = "Niemandsland";

	static {
		silentThrow(() -> {
			Files.createDirectories(WORKING_DB.getParent());
			Files.deleteIfExists(WORKING_DB);
			try (InputStream src = AccountsControllerTest.class.getResourceAsStream("/testdata.sqlite")) {
				Files.copy(src, WORKING_DB);
			}
		});
	}

	private @Inject EmbeddedServer server;
	private @Inject AccountRepository accounts;
	private @Inject BankRepository bank;

	@BeforeEach
	void setup() {
		Unirest.config().verifySsl(false);
		Unirest.config().defaultBaseUrl("http://localhost:" + server.getPort());
	}

	@Test
	void answersNameRestsAndLatestLoginOfEveryAccountAndNothingElse() {
		accounts.addIfAbsent(new Account("Stammgast", SECRET_HASH, PasswordState.CHOSEN, Role.MEMBER, Optional.of(LOGGED_IN)));

		HttpResponse<String> response = Unirest.get("/api/v1/admin/accounts").header("Cookie", LoginCookie.headerFor(server)).asString();

		JSONObject listed = accountNamed(new JSONArray(response.getBody()), "Stammgast");
		assertThat(response.getStatus()).isEqualTo(200);
		assertThat(listed.keySet()).containsExactlyInAnyOrder("name", "rests", "lastLogin");
		assertThat(listed.getString("lastLogin")).isEqualTo("2026-01-05T10:00:00Z");
		assertThat(response.getBody()).doesNotContain(SECRET_HASH);
	}

	@Test
	void answersAnAccountThatNeverLoggedInWithoutALatestLogin() {
		accounts.addIfAbsent(new Account("Neuling", SECRET_HASH, PasswordState.ISSUED, Role.MEMBER, Optional.empty()));

		HttpResponse<String> response = Unirest.get("/api/v1/admin/accounts").header("Cookie", LoginCookie.headerFor(server)).asString();

		JSONObject listed = accountNamed(new JSONArray(response.getBody()), "Neuling");
		assertThat(listed.isNull("lastLogin")).isTrue();
	}

	@Test
	void marksTheAccountOfAnAvatarThatMovedNothingAsRestingAndTheAccountOfTheLatestMoverAsNot() {
		String latestMover = latestBankMover();
		accounts.addIfAbsent(new Account(latestMover, SECRET_HASH, PasswordState.CHOSEN, Role.MEMBER, Optional.empty()));
		accounts.addIfAbsent(new Account(NEVER_MOVED, SECRET_HASH, PasswordState.CHOSEN, Role.MEMBER, Optional.empty()));

		HttpResponse<String> response = Unirest.get("/api/v1/admin/accounts").header("Cookie", LoginCookie.headerFor(server)).asString();

		JSONArray listed = new JSONArray(response.getBody());
		assertThat(accountNamed(listed, latestMover).getBoolean("rests")).isFalse();
		assertThat(accountNamed(listed, NEVER_MOVED).getBoolean("rests")).isTrue();
	}

	@Test
	void answersAResetWithTheNewPasswordAloneAndForbidsStoringIt() {
		accounts.addIfAbsent(new Account("Vergesslich", SECRET_HASH, PasswordState.CHOSEN, Role.MEMBER, Optional.empty()));

		HttpResponse<String> response = Unirest.post("/api/v1/admin/accounts/Vergesslich/reset").header("Cookie", LoginCookie.headerFor(server)).asString();

		JSONObject answered = new JSONObject(response.getBody());
		assertThat(response.getStatus()).isEqualTo(200);
		assertThat(response.getHeaders().getFirst("Cache-Control")).isEqualTo("no-store");
		assertThat(answered.keySet()).containsExactly("password");
		assertThat(answered.getString("password")).isNotBlank();
		assertThat(response.getBody()).doesNotContain(SECRET_HASH);
	}

	@Test
	void answersAResetOfANameWithoutAnAccountAsNotFound() {
		HttpResponse<String> response = Unirest.post("/api/v1/admin/accounts/Niemand/reset").header("Cookie", LoginCookie.headerFor(server)).asString();

		assertThat(response.getStatus()).isEqualTo(404);
		assertThat(response.getBody()).doesNotContain("password");
	}

	private String latestBankMover() {
		Map<String, Instant> latest = bank.latestTimestampPerAvatar();
		return latest.entrySet().stream().max(Map.Entry.comparingByValue()).orElseThrow().getKey();
	}

	private static JSONObject accountNamed(JSONArray listed, String name) {
		List<JSONObject> all = new ArrayList<>();
		for (int index = 0; index < listed.length(); index++) {
			all.add(listed.getJSONObject(index));
		}
		List<JSONObject> named = all.stream().filter(account -> account.getString("name").equals(name)).collect(Collectors.toList());
		assertThat(named).hasSize(1);
		return named.get(0);
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
