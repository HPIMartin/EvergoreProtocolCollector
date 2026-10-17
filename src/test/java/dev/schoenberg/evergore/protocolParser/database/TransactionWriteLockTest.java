package dev.schoenberg.evergore.protocolParser.database;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.schoenberg.evergore.protocolParser.LoggerSpy;
import dev.schoenberg.evergore.protocolParser.database.bank.BankDatabaseRepository;
import dev.schoenberg.evergore.protocolParser.helper.config.Configuration;

import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.silentThrow;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class TransactionWriteLockTest {
	private static final String FRESH_DB_PATH = "build/tmp/test/transactionWriteLockTest.sqlite";
	private static final Duration BUSY_TIMEOUT = Duration.ofMillis(500);

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
	void aTransactionsWorkDoesNotStartWhileAnOutsideWriterHoldsTheLock() {
		AtomicBoolean workRan = new AtomicBoolean();

		Throwable refusal = whileAnOutsideWriterHoldsTheFile(() -> database.inTransaction(() -> {
			workRan.set(true);
			return null;
		}));

		assertThat(refusal).hasStackTraceContaining("SQLITE_BUSY");
		assertThat(workRan).isFalse();
	}

	@Test
	void anEmptyLedgerBatchNeedsNoWriteLock() {
		BankDatabaseRepository tested = new BankDatabaseRepository(database);

		Throwable refusal = whileAnOutsideWriterHoldsTheFile(() -> tested.add(List.of()));

		assertThat(refusal).isNull();
	}

	private static Throwable whileAnOutsideWriterHoldsTheFile(Runnable work) {
		return silentThrow(() -> {
			try (Connection writer = DriverManager.getConnection("jdbc:sqlite:" + FRESH_DB_PATH); Statement lock = writer.createStatement()) {
				lock.execute("BEGIN IMMEDIATE");
				Throwable failure = catchThrowable(work::run);
				lock.execute("ROLLBACK");
				return failure;
			}
		});
	}

	private static Configuration configurationFor(String databasePath) {
		return new Configuration() {
			@Override
			public String getDatabasePath() {
				return databasePath;
			}

			@Override
			public Duration getDatabaseBusyTimeout() {
				return BUSY_TIMEOUT;
			}
		};
	}
}
