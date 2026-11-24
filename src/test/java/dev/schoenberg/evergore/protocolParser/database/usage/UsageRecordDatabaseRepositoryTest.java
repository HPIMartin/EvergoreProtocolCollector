package dev.schoenberg.evergore.protocolParser.database.usage;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.schoenberg.evergore.protocolParser.LoggerSpy;
import dev.schoenberg.evergore.protocolParser.businessLogic.usage.UsageKind;
import dev.schoenberg.evergore.protocolParser.businessLogic.usage.UsageRecord;
import dev.schoenberg.evergore.protocolParser.database.SqliteDatabase;
import dev.schoenberg.evergore.protocolParser.database.SqliteFile;
import dev.schoenberg.evergore.protocolParser.helper.config.Configuration;

import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.silentThrow;
import static org.assertj.core.api.Assertions.assertThat;

class UsageRecordDatabaseRepositoryTest {
	private static final String FRESH_DB_PATH = "build/tmp/test/usageRecordRepositoryTest.sqlite";
	private static final String ROW = "account_key || '|' || at_epoch_millis || '|' || what || '|' || ifnull(detail, '-')";
	private static final Instant LOGGED_IN = Instant.parse("2026-11-01T20:15:00.123Z");
	private static final Instant OPENED = Instant.parse("2026-11-01T20:16:30.456Z");

	private final SqliteFile databaseFile = new SqliteFile(FRESH_DB_PATH);
	private SqliteDatabase database;
	private UsageRecordDatabaseRepository tested;

	@BeforeEach
	void openAFreshDatabase() {
		silentThrow(() -> Files.deleteIfExists(Paths.get(FRESH_DB_PATH)));
		database = SqliteDatabase.open(configurationFor(FRESH_DB_PATH), () -> {}, new LoggerSpy());
		tested = new UsageRecordDatabaseRepository(database);
	}

	@AfterEach
	void closeTheDatabase() {
		database.close();
	}

	@Test
	void storesEveryRecordOfABatchWithItsAccountItsInstantItsKindAndItsDetail() {
		List<UsageRecord> batch = List
				.of(new UsageRecord("aurora", LOGGED_IN, UsageKind.LOGIN, Optional.empty()), new UsageRecord("aurora", OPENED, UsageKind.VIEW_BANK, Optional.of("Boreas")));

		tested.addAll(batch);

		assertThat(databaseFile.committedTexts("usage_record", ROW))
				.containsExactly("aurora|" + LOGGED_IN.toEpochMilli() + "|LOGIN|-", "aurora|" + OPENED.toEpochMilli() + "|VIEW_BANK|Boreas");
	}

	private static Configuration configurationFor(String databasePath) {
		return new Configuration() {
			@Override
			public String getDatabasePath() {
				return databasePath;
			}
		};
	}
}
