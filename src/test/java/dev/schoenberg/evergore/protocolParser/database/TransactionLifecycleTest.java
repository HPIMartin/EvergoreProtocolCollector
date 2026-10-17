package dev.schoenberg.evergore.protocolParser.database;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.DatabaseConnection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.schoenberg.evergore.protocolParser.LoggerSpy;
import dev.schoenberg.evergore.protocolParser.database.metaInformation.MetaInformationEntry;
import dev.schoenberg.evergore.protocolParser.helper.config.Configuration;

import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.silentThrow;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class TransactionLifecycleTest {
	private static final String FRESH_DB_PATH = "build/tmp/test/transactionLifecycleTest.sqlite";

	private final SqliteFile databaseFile = new SqliteFile(FRESH_DB_PATH);
	private GermanOrderConnectionSource pool;
	private SqliteDatabase database;

	@BeforeEach
	void openAFreshDatabaseOnAPoolOfItsOwn() {
		silentThrow(() -> Files.deleteIfExists(Paths.get(FRESH_DB_PATH)));
		SqliteDatabase.open(configurationFor(FRESH_DB_PATH), () -> {}, new LoggerSpy()).close();
		pool = silentThrow(() -> new GermanOrderConnectionSource("jdbc:sqlite:" + FRESH_DB_PATH));
		database = new SqliteDatabase(pool);
	}

	@AfterEach
	void closeTheDatabase() {
		database.close();
	}

	@Test
	void everyTransactionHandsItsConnectionBackToThePool() {
		for (int run = 0; run < 3; run++) {
			database.inTransaction(() -> null);
			catchThrowable(() -> database.inTransaction(() -> {
				throw new IllegalStateException("work failed");
			}));
		}
		DatabaseConnection leftOnTheThread = pool.getSpecialConnection(null);
		long connectionsOpened = pool.getCurrentConnectionsManaged();

		assertThat(leftOnTheThread).isNull();
		assertThat(connectionsOpened).isEqualTo(1);
	}

	@Test
	void anErrorThrownByTheWorkStoresNothingAndLeavesNoTransaction() {
		Dao<MetaInformationEntry, String> meta = database.dao(MetaInformationEntry.class);

		Throwable failure = catchThrowable(() -> database.inTransaction(() -> {
			meta.create(new MetaInformationEntry("first", "1"));
			throw new StackOverflowError("work overflowed");
		}));
		Throwable leftOpen = catchThrowable(databaseFile::takeAndReleaseTheExclusiveLockWithoutWaiting);
		List<Integer> committed = databaseFile.committedValues(MetaInformationEntry.TABLE, "value");

		assertThat(failure).hasRootCauseInstanceOf(StackOverflowError.class);
		assertThat(leftOpen).isNull();
		assertThat(committed).isEmpty();
	}

	@Test
	void aNestedTransactionFailsAndTheOuterOneStoresNothing() {
		Dao<MetaInformationEntry, String> meta = database.dao(MetaInformationEntry.class);

		Throwable failure = catchThrowable(() -> database.inTransaction(() -> {
			meta.create(new MetaInformationEntry("first", "1"));
			return database.inTransaction(() -> meta.create(new MetaInformationEntry("second", "2")));
		}));
		List<Integer> committed = databaseFile.committedValues(MetaInformationEntry.TABLE, "value");

		assertThat(failure).hasStackTraceContaining("cannot start a transaction within a transaction");
		assertThat(committed).isEmpty();
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
