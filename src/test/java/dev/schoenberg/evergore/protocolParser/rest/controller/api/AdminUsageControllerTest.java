package dev.schoenberg.evergore.protocolParser.rest.controller.api;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
import dev.schoenberg.evergore.protocolParser.application.UsageRecordRepository;
import dev.schoenberg.evergore.protocolParser.businessLogic.Constants;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.Account;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.PasswordState;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.Role;
import dev.schoenberg.evergore.protocolParser.businessLogic.usage.UsageKind;
import dev.schoenberg.evergore.protocolParser.businessLogic.usage.UsageRecord;
import dev.schoenberg.evergore.protocolParser.dataExtraction.PostCollectionHook;
import dev.schoenberg.evergore.protocolParser.database.PreDatabaseConnectionHook;
import dev.schoenberg.evergore.protocolParser.helper.config.Configuration;

import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.silentThrow;
import static org.assertj.core.api.Assertions.assertThat;

@MicronautTest
class AdminUsageControllerTest {
	private static final Path WORKING_DB = Paths.get("build/tmp/adminUsageController/adminUsageController.sqlite");
	private static final String HASH = "hashed:Nutzerin";

	static {
		silentThrow(() -> {
			Files.createDirectories(WORKING_DB.getParent());
			Files.deleteIfExists(WORKING_DB);
			try (InputStream src = AdminUsageControllerTest.class.getResourceAsStream("/testdata.sqlite")) {
				Files.copy(src, WORKING_DB);
			}
		});
	}

	private @Inject EmbeddedServer server;
	private @Inject AccountRepository accounts;
	private @Inject UsageRecordRepository records;

	@BeforeEach
	void setup() {
		Unirest.config().verifySsl(false);
		Unirest.config().defaultBaseUrl("http://localhost:" + server.getPort());
	}

	@Test
	void answersEachAccountsCountsInBothWindowsWithTheGuildTheAdminKeyTheFirstDayAndTheLostRecords() {
		Instant yesterday = Instant.now().minus(Duration.ofDays(1)).truncatedTo(ChronoUnit.SECONDS);
		Instant twoMonthsAgo = yesterday.minus(Duration.ofDays(60));
		accounts.addIfAbsent(new Account("Nutzerin", HASH, PasswordState.CHOSEN, Role.MEMBER, Optional.empty()));
		records
				.addAll(List
						.of(record("nutzerin", yesterday, UsageKind.LOGIN), record("nutzerin", yesterday, UsageKind.PAGE_LOAD), record("nutzerin", yesterday, UsageKind.LOGOUT),
								record("nutzerin", twoMonthsAgo, UsageKind.LOGIN), record("admin", yesterday, UsageKind.LOGIN)));

		HttpResponse<String> response = Unirest.get("/api/v1/admin/usage").header("Cookie", LoginCookie.headerFor(server)).asString();

		assertThat(response.getStatus()).isEqualTo(200);
		JSONObject usage = new JSONObject(response.getBody());
		assertThat(usage.keySet()).containsExactlyInAnyOrder("lastThirtyDays", "allTheTime", "recordedSince", "lostSinceStart");
		assertThat(usage.getString("recordedSince")).isEqualTo(twoMonthsAgo.atZone(Constants.APP_ZONE).toLocalDate().toString());
		assertThat(usage.getLong("lostSinceStart")).isZero();
		JSONObject recent = usage.getJSONObject("lastThirtyDays");
		assertThat(countsOf(recent, "Nutzerin")).isEqualTo(List.of(1, 1, 1, 1));
		assertThat(Instant.parse(accountNamed(recent, "Nutzerin").getJSONObject("counts").getString("latestUse"))).isEqualTo(yesterday);
		assertThat(countsOf(usage.getJSONObject("allTheTime"), "Nutzerin")).isEqualTo(List.of(2, 1, 1, 2));
		assertThat(recent.getJSONObject("guild").getInt("logins")).isEqualTo(1);
		assertThat(recent.getJSONObject("adminKey").getInt("logins")).isEqualTo(1);
	}

	@Test
	void answersAnAccountWithoutRecordsWithZerosAndNoLatestUse() {
		accounts.addIfAbsent(new Account("Zaungast", HASH, PasswordState.CHOSEN, Role.MEMBER, Optional.empty()));

		HttpResponse<String> response = Unirest.get("/api/v1/admin/usage").header("Cookie", LoginCookie.headerFor(server)).asString();

		assertThat(response.getStatus()).isEqualTo(200);
		JSONObject recent = new JSONObject(response.getBody()).getJSONObject("lastThirtyDays");
		assertThat(countsOf(recent, "Zaungast")).isEqualTo(List.of(0, 0, 0, 0));
		assertThat(accountNamed(recent, "Zaungast").getJSONObject("counts").isNull("latestUse")).isTrue();
	}

	private static UsageRecord record(String account, Instant at, UsageKind what) {
		return new UsageRecord(account, at, what, Optional.empty());
	}

	private static List<Integer> countsOf(JSONObject window, String name) {
		JSONObject counts = accountNamed(window, name).getJSONObject("counts");
		return List.of(counts.getInt("logins"), counts.getInt("pageLoads"), counts.getInt("logouts"), counts.getInt("daysUsed"));
	}

	private static JSONObject accountNamed(JSONObject window, String name) {
		JSONArray listed = window.getJSONArray("accounts");
		List<JSONObject> named = new ArrayList<>();
		for (int index = 0; index < listed.length(); index++) {
			if (listed.getJSONObject(index).getString("name").equals(name)) {
				named.add(listed.getJSONObject(index));
			}
		}
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
