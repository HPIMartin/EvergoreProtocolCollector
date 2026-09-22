package dev.schoenberg.evergore.protocolParser.database.storage;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.schoenberg.evergore.protocolParser.LoggerSpy;
import dev.schoenberg.evergore.protocolParser.businessLogic.base.TransferType;
import dev.schoenberg.evergore.protocolParser.businessLogic.storage.StorageEntry;
import dev.schoenberg.evergore.protocolParser.database.SqliteDatabase;
import dev.schoenberg.evergore.protocolParser.database.SqliteFile;
import dev.schoenberg.evergore.protocolParser.helper.config.Configuration;

import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.silentThrow;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class StorageDatabaseRepositoryTest {
	private static final Instant BOUNDARY = Instant.parse("2024-06-01T13:37:00Z");
	private static final Instant ONE_MINUTE_BEFORE_BOUNDARY = BOUNDARY.minusSeconds(60);
	private static final Instant ONE_MINUTE_AFTER_BOUNDARY = BOUNDARY.plusSeconds(60);
	private static final String FRESH_DB_PATH = "build/tmp/test/storageRepositoryTest.sqlite";

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
	void getAllSinceIncludesTheRowExactlyAtTheGivenTimestamp() {
		StorageDatabaseRepository repo = repository();
		StorageEntry atBoundary = storageEntry("Aurora", BOUNDARY, 3);
		repo.add(List.of(atBoundary));

		List<StorageEntry> result = repo.getAllSince(BOUNDARY);

		assertThat(result).containsExactly(atBoundary);
	}

	@Test
	void getAllSinceExcludesRowsOlderThanTheGivenTimestamp() {
		StorageDatabaseRepository repo = repository();
		repo.add(List.of(storageEntry("Aurora", ONE_MINUTE_BEFORE_BOUNDARY, 1)));

		List<StorageEntry> result = repo.getAllSince(BOUNDARY);

		assertThat(result).isEmpty();
	}

	@Test
	void getAllSinceIncludesRowsNewerThanTheGivenTimestamp() {
		StorageDatabaseRepository repo = repository();
		StorageEntry newer = storageEntry("Aurora", ONE_MINUTE_AFTER_BOUNDARY, 3);
		repo.add(List.of(newer));

		List<StorageEntry> result = repo.getAllSince(BOUNDARY);

		assertThat(result).containsExactly(newer);
	}

	@Test
	void aStoredWithdrawalReadsBackAsAWithdrawal() {
		StorageDatabaseRepository tested = repository();
		StorageEntry withdrawal = new StorageEntry(BOUNDARY, "Aurora", 1, "Drachenhaut", 80, TransferType.ENTNAHME);
		tested.add(List.of(withdrawal));

		List<StorageEntry> result = tested.getAllFor("Aurora");

		assertThat(result).containsExactly(withdrawal);
	}

	@Test
	void getAllForAnAvatarHoldsOnlyTheRowsStoredBeforeItReturned() {
		StorageDatabaseRepository tested = repository();
		StorageEntry older = storageEntry("Aurora", ONE_MINUTE_BEFORE_BOUNDARY, 1);
		StorageEntry newer = storageEntry("Aurora", BOUNDARY, 2);
		tested.add(List.of(older, newer));

		List<StorageEntry> result = tested.getAllFor("Aurora");
		tested.add(List.of(storageEntry("Aurora", ONE_MINUTE_AFTER_BOUNDARY, 3)));

		assertThat(result).containsExactlyInAnyOrder(older, newer);
	}

	@Test
	void getAllForAnAvatarFailsAtTheCallOnAStoredRowOfAnUnknownTransferType() {
		StorageDatabaseRepository tested = repository();
		tested.add(List.of(storageEntry("Aurora", BOUNDARY, 1)));
		databaseFile.execute("UPDATE " + StorageDatabaseEntry.TABLE + " SET type = 'Unbekannt'");

		Throwable failure = catchThrowable(() -> tested.getAllFor("Aurora"));

		assertThat(failure).hasMessageContaining("Unbekannt");
	}

	@Test
	void getAllForAnAvatarLeavesNothingOpenOnceItReturns() {
		StorageDatabaseRepository tested = repository();
		tested.add(List.of(storageEntry("Aurora", ONE_MINUTE_BEFORE_BOUNDARY, 1), storageEntry("Aurora", BOUNDARY, 2)));

		tested.getAllFor("Aurora");
		Throwable lockRefusal = catchThrowable(databaseFile::takeAndReleaseTheExclusiveLockWithoutWaiting);

		assertThat(lockRefusal).isNull();
	}

	@Test
	void addStoresSeveralRowsInOneCommit() {
		StorageDatabaseRepository tested = repository();
		List<StorageEntry> severalRows = List.of(storageEntry("Aurora", BOUNDARY, 1), storageEntry("Aurora", BOUNDARY, 2), storageEntry("Boreas", BOUNDARY, 3));

		int commits = databaseFile.commitsDuring(() -> tested.add(severalRows));

		assertThat(commits).isEqualTo(1);
	}

	@Test
	void addStoresNothingWhenAnEntryCannotBeMapped() {
		StorageDatabaseRepository tested = repository();
		List<StorageEntry> batch = List.of(storageEntry("Aurora", BOUNDARY, 1), entryWithoutTimestamp(2));

		Throwable failure = catchThrowable(() -> tested.add(batch));
		long stored = tested.countFor("Aurora");

		assertThat(failure).isNotNull();
		assertThat(stored).isZero();
	}

	@Test
	void aRefusedRowStoresAtMostTheRowsBeforeIt() {
		StorageDatabaseRepository tested = repository();
		List<StorageEntry> batch = List
				.of(storageEntry("Aurora", ONE_MINUTE_BEFORE_BOUNDARY, 1), storageEntry("Aurora", BOUNDARY, 2), entryWithoutAvatar(3),
						storageEntry("Aurora", ONE_MINUTE_AFTER_BOUNDARY, 4));

		Throwable failure = catchThrowable(() -> tested.add(batch));
		Throwable lockRefusal = catchThrowable(databaseFile::takeAndReleaseTheExclusiveLockWithoutWaiting);
		List<Integer> committed = databaseFile.committedValues(StorageDatabaseEntry.TABLE, "quantity");

		assertThat(failure).hasStackTraceContaining("NOT NULL constraint failed");
		assertThat(lockRefusal).isNull();
		assertThat(List.of(List.of(), List.of(1), List.of(1, 2))).contains(committed);
	}

	@Test
	void countForCountsOnlyTheRowsOfTheGivenAvatar() {
		StorageDatabaseRepository repo = repository();
		repo.add(List.of(storageEntry("Aurora", ONE_MINUTE_BEFORE_BOUNDARY, 1), storageEntry("Aurora", BOUNDARY, 2), storageEntry("Boreas", ONE_MINUTE_AFTER_BOUNDARY, 3)));

		long count = repo.countFor("Aurora");

		assertThat(count).isEqualTo(2);
	}

	@Test
	void countForReturnsZeroForAnAvatarWithoutRows() {
		StorageDatabaseRepository repo = repository();

		long count = repo.countFor("Nobody");

		assertThat(count).isZero();
	}

	@Test
	void namesTheLatestTimestampOfEveryAvatarThatHasRows() {
		StorageDatabaseRepository repo = repository();
		repo.add(List.of(storageEntry("Aurora", ONE_MINUTE_BEFORE_BOUNDARY, 1), storageEntry("Aurora", ONE_MINUTE_AFTER_BOUNDARY, 2), storageEntry("Boreas", BOUNDARY, 3)));

		Map<String, Instant> latest = repo.latestTimestampPerAvatar();

		assertThat(latest).containsExactlyInAnyOrderEntriesOf(Map.of("Aurora", ONE_MINUTE_AFTER_BOUNDARY, "Boreas", BOUNDARY));
	}

	@Test
	void namesNobodyWhileTheLedgerHasNoRowAtAll() {
		Map<String, Instant> latest = repository().latestTimestampPerAvatar();

		assertThat(latest).isEmpty();
	}

	private static StorageEntry storageEntry(String avatar, Instant timeStamp, int quantity) {
		return new StorageEntry(timeStamp, avatar, quantity, "Drachenhaut", 80, TransferType.EINLAGERUNG);
	}

	private static StorageEntry entryWithoutTimestamp(int quantity) {
		return new StorageEntry(null, "Aurora", quantity, "Drachenhaut", 80, TransferType.EINLAGERUNG);
	}

	private static StorageEntry entryWithoutAvatar(int quantity) {
		return new StorageEntry(BOUNDARY, null, quantity, "Drachenhaut", 80, TransferType.EINLAGERUNG);
	}

	private static Configuration configurationFor(String databasePath) {
		return new Configuration() {
			@Override
			public String getDatabasePath() {
				return databasePath;
			}
		};
	}

	private StorageDatabaseRepository repository() {
		return new StorageDatabaseRepository(database);
	}
}
