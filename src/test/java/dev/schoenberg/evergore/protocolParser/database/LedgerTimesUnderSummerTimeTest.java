package dev.schoenberg.evergore.protocolParser.database;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.field.DataPersister;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.schoenberg.evergore.protocolParser.LoggerSpy;
import dev.schoenberg.evergore.protocolParser.businessLogic.banking.BankEntry;
import dev.schoenberg.evergore.protocolParser.businessLogic.banking.BankSortKey;
import dev.schoenberg.evergore.protocolParser.businessLogic.base.LedgerSort;
import dev.schoenberg.evergore.protocolParser.businessLogic.base.TransferType;
import dev.schoenberg.evergore.protocolParser.database.bank.BankDatabaseEntry;
import dev.schoenberg.evergore.protocolParser.database.bank.BankDatabaseRepository;
import dev.schoenberg.evergore.protocolParser.helper.config.Configuration;

import static dev.schoenberg.evergore.protocolParser.businessLogic.base.SortDirection.DESCENDING;
import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.silentThrow;
import static org.assertj.core.api.Assertions.assertThat;

class LedgerTimesUnderSummerTimeTest {
	private static final String FRESH_DB_PATH = "build/tmp/test/ledgerTimesUnderSummerTimeTest.sqlite";
	private static final TimeZone ZONE_WITH_SUMMER_TIME = TimeZone.getTimeZone("Europe/Berlin");
	private static final Instant FIRST_PASS_OF_TWO_THIRTY = Instant.parse("2026-10-25T00:30:00Z");
	private static final Instant SECOND_PASS_OF_TWO_THIRTY = Instant.parse("2026-10-25T01:30:00Z");
	private static final Instant TWO_FORTY_FIVE_IN_SUMMER_TIME = Instant.parse("2026-10-25T00:45:00Z");
	private static final Instant TWO_FIFTEEN_IN_WINTER_TIME = Instant.parse("2026-10-25T01:15:00Z");

	private final SqliteFile databaseFile = new SqliteFile(FRESH_DB_PATH);
	private TimeZone zoneBefore;
	private SqliteDatabase database;

	@BeforeEach
	void openAFreshDatabaseInAZoneWithSummerTime() {
		zoneBefore = TimeZone.getDefault();
		TimeZone.setDefault(ZONE_WITH_SUMMER_TIME);
		silentThrow(() -> Files.deleteIfExists(Paths.get(FRESH_DB_PATH)));
		database = SqliteDatabase.open(configurationFor(FRESH_DB_PATH), () -> {}, new LoggerSpy());
	}

	@AfterEach
	void restoreTheZoneAndCloseTheDatabase() {
		TimeZone.setDefault(zoneBefore);
		if (database != null) {
			database.close();
		}
	}

	@Test
	void theTimestampColumnIsWrittenByTheUtcPersister() {
		Dao<BankDatabaseEntry, String> rows = database.dao(BankDatabaseEntry.class);

		DataPersister persister = rows.getTableInfo().getFieldTypeByColumnName(LedgerDatabaseEntry.TIMESTAMP_COLUMN).getDataPersister();

		assertThat(persister).isSameAs(UtcDateStringType.getSingleton());
	}

	@Test
	void theTimestampTextIsWrittenInUtcWithItsMillisecondsAsSixDigitsBesideTheInstant() {
		BankDatabaseRepository tested = repository();

		tested.add(List.of(deposit(SECOND_PASS_OF_TWO_THIRTY.plusMillis(123))));
		List<String> stored = databaseFile.committedTexts(BankDatabaseEntry.TABLE, "timeStamp || '|' || timeStampEpochMillis");

		assertThat(stored).containsExactly("2026-10-25 01:30:00.000123|1792891800123");
	}

	@Test
	void bothPassesOfTheHourTheClocksGoBackReadBackApart() {
		BankDatabaseRepository tested = repository();
		tested.add(List.of(deposit(FIRST_PASS_OF_TWO_THIRTY), deposit(SECOND_PASS_OF_TWO_THIRTY)));

		List<BankEntry> read = tested.getAllFor("Aurora");

		assertThat(read).extracting(BankEntry::timeStamp).containsExactlyInAnyOrder(FIRST_PASS_OF_TWO_THIRTY, SECOND_PASS_OF_TWO_THIRTY);
	}

	@Test
	void theIngestWindowFromTheSecondPassLeavesTheFirstPassOut() {
		BankDatabaseRepository tested = repository();
		tested.add(List.of(deposit(FIRST_PASS_OF_TWO_THIRTY), deposit(SECOND_PASS_OF_TWO_THIRTY)));

		List<BankEntry> sinceTheSecondPass = tested.getAllSince(SECOND_PASS_OF_TWO_THIRTY);

		assertThat(sinceTheSecondPass).extracting(BankEntry::timeStamp).containsExactly(SECOND_PASS_OF_TWO_THIRTY);
	}

	@Test
	void theLatestMovementIsTwoFifteenInWinterTimeNotTwoFortyFiveInSummerTime() {
		BankDatabaseRepository tested = repository();
		tested.add(List.of(deposit(TWO_FIFTEEN_IN_WINTER_TIME), deposit(TWO_FORTY_FIVE_IN_SUMMER_TIME)));

		Map<String, Instant> latest = tested.latestTimestampPerAvatar();

		assertThat(latest).containsExactly(Map.entry("Aurora", TWO_FIFTEEN_IN_WINTER_TIME));
	}

	@Test
	void aPageNewestFirstPutsTwoFifteenInWinterTimeBeforeTwoFortyFiveInSummerTime() {
		BankDatabaseRepository tested = repository();
		tested.add(List.of(deposit(TWO_FORTY_FIVE_IN_SUMMER_TIME), deposit(TWO_FIFTEEN_IN_WINTER_TIME)));

		List<BankEntry> page = tested.getAllFor("Aurora", new LedgerSort<>(BankSortKey.TIMESTAMP, DESCENDING), 0, 2);

		assertThat(page).extracting(BankEntry::timeStamp).containsExactly(TWO_FIFTEEN_IN_WINTER_TIME, TWO_FORTY_FIVE_IN_SUMMER_TIME);
	}

	private BankDatabaseRepository repository() {
		return new BankDatabaseRepository(database);
	}

	private static BankEntry deposit(Instant at) {
		return new BankEntry(at, "Aurora", 100, TransferType.EINLAGERUNG);
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
