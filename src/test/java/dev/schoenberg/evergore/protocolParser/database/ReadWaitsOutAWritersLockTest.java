package dev.schoenberg.evergore.protocolParser.database;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.schoenberg.evergore.protocolParser.LoggerSpy;
import dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformation;
import dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey;
import dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationSnapshot;
import dev.schoenberg.evergore.protocolParser.database.metaInformation.MetaInformationDatabaseRepository;
import dev.schoenberg.evergore.protocolParser.helper.config.Configuration;

import static dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey.getBankPlacement;
import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.silentThrow;
import static org.assertj.core.api.Assertions.assertThat;

class ReadWaitsOutAWritersLockTest {
	private static final String FRESH_DB_PATH = "build/tmp/test/readWaitsOutAWritersLockTest.sqlite";
	private static final Duration LOCK_HELD_PAST_THE_DRIVERS_DEFAULT_WAIT = Duration.ofSeconds(4);
	private static final Duration HANG_GUARD = Duration.ofSeconds(30);
	private static final MetaInformationKey<Long> SEEDED_KEY = getBankPlacement("Aurora");

	private final CountDownLatch readerIsAboutToRead = new CountDownLatch(1);

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
	void aReadLandingOnAnotherConnectionsExclusiveLockAnswersOnlyOnceTheLockIsReleased() {
		MetaInformationDatabaseRepository tested = new MetaInformationDatabaseRepository(database);
		tested.add(List.of(new MetaInformation<>(SEEDED_KEY, 1337L)));

		ReadUnderALock read = snapshotWhileAnotherConnectionHoldsTheExclusiveLock(tested);

		assertThat(read.answer()).isCompleted();
		assertThat(read.answer().join().get(SEEDED_KEY)).contains(1337L);
		assertThat(read.answeredWhileTheLockWasHeld()).isFalse();
	}

	private ReadUnderALock snapshotWhileAnotherConnectionHoldsTheExclusiveLock(MetaInformationDatabaseRepository tested) {
		CompletableFuture<MetaInformationSnapshot> answer = new CompletableFuture<>();
		Thread reader = new Thread(() -> {
			readerIsAboutToRead.countDown();
			try {
				answer.complete(tested.snapshot());
			} catch (RuntimeException failure) {
				answer.completeExceptionally(failure);
			}
		});

		return silentThrow(() -> {
			boolean answeredWhileTheLockWasHeld;
			try (Connection writer = DriverManager.getConnection("jdbc:sqlite:" + FRESH_DB_PATH); Statement lock = writer.createStatement()) {
				lock.execute("BEGIN EXCLUSIVE");
				reader.start();
				assertThat(readerIsAboutToRead.await(HANG_GUARD.toSeconds(), TimeUnit.SECONDS)).as("the reader never started").isTrue();
				Thread.sleep(LOCK_HELD_PAST_THE_DRIVERS_DEFAULT_WAIT);
				answeredWhileTheLockWasHeld = answer.isDone();
				lock.execute("ROLLBACK");
			}
			reader.join(HANG_GUARD);
			return new ReadUnderALock(answer, answeredWhileTheLockWasHeld);
		});
	}

	private record ReadUnderALock(CompletableFuture<MetaInformationSnapshot> answer, boolean answeredWhileTheLockWasHeld) {}

	private static Configuration configurationFor(String databasePath) {
		return new Configuration() {
			@Override
			public String getDatabasePath() {
				return databasePath;
			}
		};
	}
}
