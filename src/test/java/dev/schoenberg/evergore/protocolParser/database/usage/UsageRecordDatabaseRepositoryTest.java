package dev.schoenberg.evergore.protocolParser.database.usage;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
import static org.assertj.core.api.Assertions.catchThrowable;

class UsageRecordDatabaseRepositoryTest {
	private static final String FRESH_DB_PATH = "build/tmp/test/usageRecordRepositoryTest.sqlite";
	private static final String ROW = "account_key || '|' || at_epoch_millis || '|' || what || '|' || ifnull(detail, '-')";
	private static final Instant LOGGED_IN = Instant.parse("2026-11-01T20:15:00.123Z");
	private static final Instant OPENED = Instant.parse("2026-11-01T20:16:30.456Z");
	private static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");

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

	@Test
	void givesEachStoredRecordTheNextIdAcrossBatches() {
		tested.addAll(List.of(new UsageRecord("aurora", LOGGED_IN, UsageKind.LOGIN, Optional.empty())));
		tested.addAll(List.of(new UsageRecord("boreas", OPENED, UsageKind.LOGIN, Optional.empty()), new UsageRecord("boreas", OPENED, UsageKind.PAGE_LOAD, Optional.empty())));

		assertThat(databaseFile.committedTexts("usage_record", "ifnull(id, '-')")).containsExactly("1", "2", "3");
	}

	@Test
	void writesABatchInOneCommit() {
		List<UsageRecord> batch = List
				.of(new UsageRecord("aurora", LOGGED_IN, UsageKind.LOGIN, Optional.empty()), new UsageRecord("aurora", LOGGED_IN, UsageKind.PAGE_LOAD, Optional.empty()),
						new UsageRecord("aurora", OPENED, UsageKind.LOGOUT, Optional.empty()));

		int commits = databaseFile.commitsDuring(() -> tested.addAll(batch));

		assertThat(commits).isEqualTo(1);
	}

	@Test
	void aBatchWithARefusedRecordStoresNoneOfItsRecords() {
		List<UsageRecord> batch = List
				.of(new UsageRecord("aurora", LOGGED_IN, UsageKind.LOGIN, Optional.empty()), new UsageRecord(null, OPENED, UsageKind.PAGE_LOAD, Optional.empty()));

		Throwable refusal = catchThrowable(() -> tested.addAll(batch));

		assertThat(refusal).isNotNull();
		assertThat(databaseFile.committedTexts("usage_record", ROW)).isEmpty();
	}

	@Test
	void countsTheRecordsOfEachAccountAndKindFromTheGivenMomentOn() {
		Instant from = Instant.parse("2026-11-10T11:00:00Z");
		tested
				.addAll(List
						.of(record("aurora", from.minusMillis(1), UsageKind.LOGIN), record("aurora", from, UsageKind.LOGIN),
								record("aurora", from.plusSeconds(60), UsageKind.PAGE_LOAD), record("aurora", from.plusSeconds(120), UsageKind.PAGE_LOAD),
								record("boreas", from.plusSeconds(60), UsageKind.LOGOUT)));

		Map<String, Map<UsageKind, Integer>> counts = tested.countsSince(from);

		assertThat(counts).isEqualTo(Map.of("aurora", Map.of(UsageKind.LOGIN, 1, UsageKind.PAGE_LOAD, 2), "boreas", Map.of(UsageKind.LOGOUT, 1)));
	}

	@Test
	void countsTwoUsesOnEitherSideOfMidnightInBerlinAsTwoDaysFromTheGivenMomentOn() {
		Instant from = Instant.parse("2026-01-09T12:00:00Z");
		tested
				.addAll(List
						.of(record("aurora", from.minusMillis(1), UsageKind.LOGIN), record("aurora", Instant.parse("2026-01-10T22:30:00Z"), UsageKind.LOGIN),
								record("aurora", Instant.parse("2026-01-10T23:30:00Z"), UsageKind.PAGE_LOAD)));

		Map<String, Set<LocalDate>> days = tested.daysUsedSince(from, BERLIN);

		assertThat(days).isEqualTo(Map.of("aurora", Set.of(LocalDate.parse("2026-01-10"), LocalDate.parse("2026-01-11"))));
	}

	@Test
	void countsTheLongNightOfTheEndOfSummerTimeInBerlinAsOneDay() {
		tested.addAll(List.of(record("aurora", Instant.parse("2026-10-24T22:30:00Z"), UsageKind.LOGIN), record("aurora", Instant.parse("2026-10-25T22:30:00Z"), UsageKind.LOGOUT)));

		Map<String, Set<LocalDate>> days = tested.daysUsedSince(Instant.EPOCH, BERLIN);

		assertThat(days).isEqualTo(Map.of("aurora", Set.of(LocalDate.parse("2026-10-25"))));
	}

	@Test
	void countsNoDayForANoticeUnderstood() {
		tested
				.addAll(List
						.of(record("aurora", Instant.parse("2026-01-10T12:00:00Z"), UsageKind.NOTICE_UNDERSTOOD),
								record("aurora", Instant.parse("2026-01-12T12:00:00Z"), UsageKind.LOGIN)));

		Map<String, Set<LocalDate>> days = tested.daysUsedSince(Instant.EPOCH, BERLIN);

		assertThat(days).isEqualTo(Map.of("aurora", Set.of(LocalDate.parse("2026-01-12"))));
	}

	private static UsageRecord record(String accountKey, Instant at, UsageKind kind) {
		return new UsageRecord(accountKey, at, kind, Optional.empty());
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
