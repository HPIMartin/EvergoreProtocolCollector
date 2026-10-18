package dev.schoenberg.evergore.protocolParser.database;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.schoenberg.evergore.protocolParser.LoggerSpy;
import dev.schoenberg.evergore.protocolParser.businessLogic.banking.BankEntry;
import dev.schoenberg.evergore.protocolParser.businessLogic.banking.BankSortKey;
import dev.schoenberg.evergore.protocolParser.businessLogic.base.LedgerSort;
import dev.schoenberg.evergore.protocolParser.database.bank.BankDatabaseEntry;
import dev.schoenberg.evergore.protocolParser.database.bank.BankDatabaseRepository;
import dev.schoenberg.evergore.protocolParser.helper.config.Configuration;

import static dev.schoenberg.evergore.protocolParser.businessLogic.base.SortDirection.DESCENDING;
import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.silentThrow;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class LedgerReadsTheInstantTest {
	private static final String FRESH_DB_PATH = "build/tmp/test/ledgerReadsTheInstantTest.sqlite";
	private static final Instant MID_JULY = Instant.parse("2026-07-15T12:00:00Z");
	private static final Instant MID_FEBRUARY = Instant.parse("2026-02-15T12:00:00Z");
	private static final Instant FIRST_OF_MARCH = Instant.parse("2026-03-01T00:00:00Z");
	private static final String ROW_WITHOUT_INSTANT = "00000000-0000-0000-0000-000000000005";

	private final SqliteFile databaseFile = new SqliteFile(FRESH_DB_PATH);
	private SqliteDatabase database;

	@BeforeEach
	void openAFreshDatabaseWithTwoRowsWhoseTextAndInstantDisagree() {
		silentThrow(() -> Files.deleteIfExists(Paths.get(FRESH_DB_PATH)));
		database = SqliteDatabase.open(configurationFor(FRESH_DB_PATH), () -> {}, new LoggerSpy());
		storeRow("00000000-0000-0000-0000-000000000007", "2026-01-01 12:00:00.000000", MID_JULY);
		storeRow("00000000-0000-0000-0000-000000000002", "2026-08-01 12:00:00.000000", MID_FEBRUARY);
	}

	@AfterEach
	void closeTheDatabase() {
		database.close();
	}

	@Test
	void aMovementReadsBackAtItsInstant() {
		BankDatabaseRepository tested = repository();

		List<BankEntry> read = tested.getAllFor("Aurora");

		assertThat(read).extracting(BankEntry::timeStamp).containsExactlyInAnyOrder(MID_JULY, MID_FEBRUARY);
	}

	@Test
	void aPageNewestFirstFollowsTheInstants() {
		BankDatabaseRepository tested = repository();

		List<BankEntry> page = tested.getAllFor("Aurora", new LedgerSort<>(BankSortKey.TIMESTAMP, DESCENDING), 0, 2);

		assertThat(page).extracting(BankEntry::timeStamp).containsExactly(MID_JULY, MID_FEBRUARY);
	}

	@Test
	void theIngestWindowFollowsTheInstants() {
		BankDatabaseRepository tested = repository();

		List<BankEntry> sinceMarch = tested.getAllSince(FIRST_OF_MARCH);

		assertThat(sinceMarch).extracting(BankEntry::timeStamp).containsExactly(MID_JULY);
	}

	@Test
	void theLatestMovementIsTheLatestInstant() {
		BankDatabaseRepository tested = repository();

		Map<String, Instant> latest = tested.latestTimestampPerAvatar();

		assertThat(latest).containsExactly(Map.entry("Aurora", MID_JULY));
	}

	@Test
	void aRowWithoutItsInstantFailsTheReadNamingTheRow() {
		BankDatabaseRepository tested = repository();
		databaseFile
				.execute("INSERT INTO " + BankDatabaseEntry.TABLE + " (id, timeStamp, avatar, amount, type) VALUES ('" + ROW_WITHOUT_INSTANT
						+ "', '2026-05-01 12:00:00.000000', 'Aurora', 100, 'Einlagerung')");

		Throwable failure = catchThrowable(() -> tested.getAllFor("Aurora"));

		assertThat(failure).hasMessageContaining(ROW_WITHOUT_INSTANT);
	}

	private void storeRow(String id, String text, Instant instant) {
		databaseFile
				.execute("INSERT INTO " + BankDatabaseEntry.TABLE + " (id, timeStamp, timeStampEpochMillis, avatar, amount, type) VALUES ('" + id + "', '" + text + "', "
						+ instant.toEpochMilli() + ", 'Aurora', 100, 'Einlagerung')");
	}

	private BankDatabaseRepository repository() {
		return new BankDatabaseRepository(database);
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
