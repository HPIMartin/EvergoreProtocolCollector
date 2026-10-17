package dev.schoenberg.evergore.protocolParser.database;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.schoenberg.evergore.protocolParser.LoggerSpy;
import dev.schoenberg.evergore.protocolParser.businessLogic.banking.BankEntry;
import dev.schoenberg.evergore.protocolParser.businessLogic.base.TransferType;
import dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformation;
import dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey;
import dev.schoenberg.evergore.protocolParser.database.bank.BankDatabaseEntry;
import dev.schoenberg.evergore.protocolParser.database.bank.BankDatabaseRepository;
import dev.schoenberg.evergore.protocolParser.database.metaInformation.MetaInformationDatabaseRepository;
import dev.schoenberg.evergore.protocolParser.database.metaInformation.MetaInformationEntry;
import dev.schoenberg.evergore.protocolParser.helper.config.Configuration;

import static dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey.getBankPlacement;
import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.silentThrow;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class RefusedCommitTest {
	private static final String FRESH_DB_PATH = "build/tmp/test/refusedCommitTest.sqlite";
	private static final Duration SHORT_BUSY_TIMEOUT = Duration.ofMillis(100);
	private static final Instant STAMP = Instant.parse("2024-06-01T13:37:00Z");
	private static final MetaInformationKey<Long> KEY = getBankPlacement("Aurora");
	private static final String ROLLBACK_MESSAGE = "rolled back by SQLite itself";

	private final SqliteFile databaseFile = new SqliteFile(FRESH_DB_PATH);
	private SqliteDatabase database;

	@BeforeEach
	void openAFreshDatabase() {
		silentThrow(() -> Files.deleteIfExists(Paths.get(FRESH_DB_PATH)));
		database = SqliteDatabase.open(configurationFor(FRESH_DB_PATH), () -> {}, new LoggerSpy());
	}

	@AfterEach
	void closeTheDatabase() {
		database.close();
	}

	@Test
	void aLedgerBatchWhoseCommitAnOutsideReaderRefusesLeavesNoTransactionAndNoneOfItsRows() {
		BankDatabaseRepository tested = new BankDatabaseRepository(database);

		Throwable refusal = whileAnOutsideReaderHoldsTheFile(() -> tested.add(List.of(bankEntry(1), bankEntry(2))));
		Throwable leftOpen = catchThrowable(databaseFile::takeAndReleaseTheExclusiveLockWithoutWaiting);
		long readByTheService = tested.countFor("Aurora");
		List<Integer> committed = databaseFile.committedValues(BankDatabaseEntry.TABLE, "amount");

		assertThat(refusal).hasStackTraceContaining("SQLITE_BUSY");
		assertThat(leftOpen).isNull();
		assertThat(readByTheService).isZero();
		assertThat(committed).isEmpty();
	}

	@Test
	void aLedgerBatchAfterARefusedCommitIsStored() {
		BankDatabaseRepository tested = new BankDatabaseRepository(database);
		whileAnOutsideReaderHoldsTheFile(() -> tested.add(List.of(bankEntry(1))));

		tested.add(List.of(bankEntry(2)));
		List<Integer> committed = databaseFile.committedValues(BankDatabaseEntry.TABLE, "amount");

		assertThat(committed).containsExactly(2);
	}

	@Test
	void aMetaAddWhoseCommitAnOutsideReaderRefusesLeavesNoTransactionAndNoneOfItsValues() {
		MetaInformationDatabaseRepository tested = new MetaInformationDatabaseRepository(database);

		Throwable refusal = whileAnOutsideReaderHoldsTheFile(() -> tested.add(List.of(new MetaInformation<>(KEY, 1337L))));
		Throwable leftOpen = catchThrowable(databaseFile::takeAndReleaseTheExclusiveLockWithoutWaiting);
		Optional<Long> readByTheService = tested.snapshot().get(KEY);
		List<Integer> committed = databaseFile.committedValues(MetaInformationEntry.TABLE, "value");

		assertThat(refusal).hasStackTraceContaining("SQLITE_BUSY");
		assertThat(leftOpen).isNull();
		assertThat(readByTheService).isEmpty();
		assertThat(committed).isEmpty();
	}

	@Test
	void aMetaAddAfterARefusedCommitIsStored() {
		MetaInformationDatabaseRepository tested = new MetaInformationDatabaseRepository(database);
		whileAnOutsideReaderHoldsTheFile(() -> tested.add(List.of(new MetaInformation<>(KEY, 1337L))));

		tested.add(List.of(new MetaInformation<>(KEY, 42L)));
		List<Integer> committed = databaseFile.committedValues(MetaInformationEntry.TABLE, "value");

		assertThat(committed).containsExactly(42);
	}

	@Test
	void aLedgerBatchThatSqliteRollsBackItselfFailsWithItsOwnCauseAndStoresNothing() {
		BankDatabaseRepository tested = new BankDatabaseRepository(database);
		databaseFile.execute(rollingBackTrigger(BankDatabaseEntry.TABLE, "NEW.amount = 2"));

		Throwable failure = catchThrowable(() -> tested.add(List.of(bankEntry(1), bankEntry(2), bankEntry(3))));
		Throwable leftOpen = catchThrowable(databaseFile::takeAndReleaseTheExclusiveLockWithoutWaiting);
		List<Integer> committed = databaseFile.committedValues(BankDatabaseEntry.TABLE, "amount");

		assertThat(failure).hasStackTraceContaining(ROLLBACK_MESSAGE);
		assertThat(failure).hasStackTraceContaining("cannot rollback - no transaction is active");
		assertThat(failure).rootCause().hasMessageContaining(ROLLBACK_MESSAGE);
		assertThat(leftOpen).isNull();
		assertThat(committed).isEmpty();
	}

	@Test
	void aMetaAddThatSqliteRollsBackItselfFailsWithItsOwnCauseAndStoresNothing() {
		MetaInformationDatabaseRepository tested = new MetaInformationDatabaseRepository(database);
		databaseFile.execute(rollingBackTrigger(MetaInformationEntry.TABLE, "NEW.value = '2'"));
		List<MetaInformation<Long>> batch = List
				.of(new MetaInformation<>(KEY, 1L), new MetaInformation<>(getBankPlacement("Boreas"), 2L), new MetaInformation<>(getBankPlacement("Ceres"), 3L));

		Throwable failure = catchThrowable(() -> tested.add(batch));
		Throwable leftOpen = catchThrowable(databaseFile::takeAndReleaseTheExclusiveLockWithoutWaiting);
		List<Integer> committed = databaseFile.committedValues(MetaInformationEntry.TABLE, "value");

		assertThat(failure).hasStackTraceContaining(ROLLBACK_MESSAGE);
		assertThat(failure).hasStackTraceContaining("cannot rollback - no transaction is active");
		assertThat(failure).rootCause().hasMessageContaining(ROLLBACK_MESSAGE);
		assertThat(leftOpen).isNull();
		assertThat(committed).isEmpty();
	}

	private static Throwable whileAnOutsideReaderHoldsTheFile(Runnable work) {
		return silentThrow(() -> {
			try (Connection reader = DriverManager.getConnection("jdbc:sqlite:" + FRESH_DB_PATH); Statement read = reader.createStatement()) {
				read.execute("BEGIN");
				read.executeQuery("SELECT count(*) FROM " + BankDatabaseEntry.TABLE).close();
				Throwable refusal = catchThrowable(work::run);
				read.execute("ROLLBACK");
				return refusal;
			}
		});
	}

	private static String rollingBackTrigger(String table, String condition) {
		return "CREATE TRIGGER roll_back_" + table + " BEFORE INSERT ON " + table + " WHEN " + condition + " BEGIN SELECT RAISE(ROLLBACK, '" + ROLLBACK_MESSAGE + "'); END";
	}

	private static BankEntry bankEntry(int amount) {
		return new BankEntry(STAMP, "Aurora", amount, TransferType.EINLAGERUNG);
	}

	private static Configuration configurationFor(String databasePath) {
		return new Configuration() {
			@Override
			public String getDatabasePath() {
				return databasePath;
			}

			@Override
			public Duration getDatabaseBusyTimeout() {
				return SHORT_BUSY_TIMEOUT;
			}
		};
	}
}
