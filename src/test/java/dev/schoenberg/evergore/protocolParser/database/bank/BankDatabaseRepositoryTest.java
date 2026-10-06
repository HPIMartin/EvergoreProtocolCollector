package dev.schoenberg.evergore.protocolParser.database.bank;

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
import dev.schoenberg.evergore.protocolParser.businessLogic.base.TransferType;
import dev.schoenberg.evergore.protocolParser.database.SqliteDatabase;
import dev.schoenberg.evergore.protocolParser.database.SqliteFile;
import dev.schoenberg.evergore.protocolParser.helper.config.Configuration;

import static dev.schoenberg.evergore.protocolParser.businessLogic.base.SortDirection.ASCENDING;
import static dev.schoenberg.evergore.protocolParser.businessLogic.base.SortDirection.DESCENDING;
import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.silentThrow;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class BankDatabaseRepositoryTest {
	private static final String FRESH_DB_PATH = "build/tmp/test/bankRepositoryTest.sqlite";
	private static final Instant BOUNDARY = Instant.parse("2024-06-01T13:37:00Z");
	private static final Instant ONE_MINUTE_BEFORE_BOUNDARY = BOUNDARY.minusSeconds(60);
	private static final Instant ONE_MINUTE_AFTER_BOUNDARY = BOUNDARY.plusSeconds(60);

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
	void freshlyConstructedRepositoryIsImmediatelyUsableWithoutSeparateInit() {
		BankDatabaseRepository repo = repository();
		BankEntry stored = bankEntry("Aurora", BOUNDARY, 1000);
		repo.add(List.of(stored));

		List<BankEntry> found = repo.getAllFor("Aurora", new LedgerSort<>(BankSortKey.TIMESTAMP, DESCENDING), 0, 10);

		assertThat(found).containsExactly(stored);
	}

	@Test
	void aPageSortedByAmountStartsWithTheLargestAmountOfTheWholeLedger() {
		BankDatabaseRepository repo = repository();
		repo.add(List.of(bankEntry("Aurora", ONE_MINUTE_BEFORE_BOUNDARY, 500), bankEntry("Aurora", BOUNDARY, 20), bankEntry("Aurora", ONE_MINUTE_AFTER_BOUNDARY, 10)));

		List<BankEntry> firstPage = repo.getAllFor("Aurora", new LedgerSort<>(BankSortKey.AMOUNT, DESCENDING), 0, 1);

		assertThat(firstPage).extracting(BankEntry::amount).containsExactly(500);
	}

	@Test
	void aBankPageSortedByTheTransferPutsDepositsFirstThenNewestFirst() {
		BankDatabaseRepository repo = repository();
		BankEntry olderWithdrawal = new BankEntry(ONE_MINUTE_BEFORE_BOUNDARY.minusSeconds(60), "Aurora", 5, TransferType.ENTNAHME);
		BankEntry olderDeposit = bankEntry("Aurora", ONE_MINUTE_BEFORE_BOUNDARY, 50);
		BankEntry newerDeposit = bankEntry("Aurora", BOUNDARY, 7);
		BankEntry newerWithdrawal = new BankEntry(ONE_MINUTE_AFTER_BOUNDARY, "Aurora", 1000, TransferType.ENTNAHME);
		repo.add(List.of(olderWithdrawal, olderDeposit, newerDeposit, newerWithdrawal));

		List<BankEntry> sorted = repo.getAllFor("Aurora", new LedgerSort<>(BankSortKey.TRANSFER_TYPE, ASCENDING), 0, 4);

		assertThat(sorted).containsExactly(newerDeposit, olderDeposit, newerWithdrawal, olderWithdrawal);
	}

	@Test
	void aBankPageSortedByTimeAscendingStartsWithTheOldestMovement() {
		BankDatabaseRepository repo = repository();
		repo.add(List.of(bankEntry("Aurora", BOUNDARY, 20), bankEntry("Aurora", ONE_MINUTE_AFTER_BOUNDARY, 10), bankEntry("Aurora", ONE_MINUTE_BEFORE_BOUNDARY, 500)));

		List<BankEntry> sorted = repo.getAllFor("Aurora", new LedgerSort<>(BankSortKey.TIMESTAMP, ASCENDING), 0, 3);

		assertThat(sorted).extracting(BankEntry::amount).containsExactly(500, 20, 10);
	}

	@Test
	void sortingTheBankByTheAvatarKeepsItNewestFirstSinceEveryRowIsThatAvatars() {
		BankDatabaseRepository repo = repository();
		repo.add(List.of(bankEntry("Aurora", BOUNDARY, 20), bankEntry("Aurora", ONE_MINUTE_AFTER_BOUNDARY, 10), bankEntry("Aurora", ONE_MINUTE_BEFORE_BOUNDARY, 500)));

		List<BankEntry> sorted = repo.getAllFor("Aurora", new LedgerSort<>(BankSortKey.AVATAR, ASCENDING), 0, 3);

		assertThat(sorted).extracting(BankEntry::amount).containsExactly(10, 20, 500);
	}

	@Test
	void getAllSinceIncludesTheRowExactlyAtTheGivenTimestamp() {
		BankDatabaseRepository repo = repository();
		BankEntry atBoundary = bankEntry("Aurora", BOUNDARY, 100);
		repo.add(List.of(atBoundary));

		List<BankEntry> result = repo.getAllSince(BOUNDARY);

		assertThat(result).containsExactly(atBoundary);
	}

	@Test
	void getAllSinceExcludesRowsOlderThanTheGivenTimestamp() {
		BankDatabaseRepository repo = repository();
		repo.add(List.of(bankEntry("Aurora", ONE_MINUTE_BEFORE_BOUNDARY, 5)));

		List<BankEntry> result = repo.getAllSince(BOUNDARY);

		assertThat(result).isEmpty();
	}

	@Test
	void getAllSinceIncludesRowsNewerThanTheGivenTimestamp() {
		BankDatabaseRepository repo = repository();
		BankEntry newer = bankEntry("Aurora", ONE_MINUTE_AFTER_BOUNDARY, 100);
		repo.add(List.of(newer));

		List<BankEntry> result = repo.getAllSince(BOUNDARY);

		assertThat(result).containsExactly(newer);
	}

	@Test
	void aStoredWithdrawalReadsBackAsAWithdrawal() {
		BankDatabaseRepository tested = repository();
		BankEntry withdrawal = new BankEntry(BOUNDARY, "Aurora", 100, TransferType.ENTNAHME);
		tested.add(List.of(withdrawal));

		List<BankEntry> result = tested.getAllFor("Aurora");

		assertThat(result).containsExactly(withdrawal);
	}

	@Test
	void getAllForAnAvatarHoldsOnlyTheRowsStoredBeforeItReturned() {
		BankDatabaseRepository tested = repository();
		BankEntry older = bankEntry("Aurora", ONE_MINUTE_BEFORE_BOUNDARY, 100);
		BankEntry newer = bankEntry("Aurora", BOUNDARY, 200);
		tested.add(List.of(older, newer));

		List<BankEntry> result = tested.getAllFor("Aurora");
		tested.add(List.of(bankEntry("Aurora", ONE_MINUTE_AFTER_BOUNDARY, 300)));

		assertThat(result).containsExactlyInAnyOrder(older, newer);
	}

	@Test
	void getAllForAnAvatarFailsAtTheCallOnAStoredRowOfAnUnknownTransferType() {
		BankDatabaseRepository tested = repository();
		tested.add(List.of(bankEntry("Aurora", BOUNDARY, 100)));
		databaseFile.execute("UPDATE " + BankDatabaseEntry.TABLE + " SET type = 'Unbekannt'");

		Throwable failure = catchThrowable(() -> tested.getAllFor("Aurora"));

		assertThat(failure).hasMessageContaining("Unbekannt");
	}

	@Test
	void getAllForAnAvatarLeavesNothingOpenOnceItReturns() {
		BankDatabaseRepository tested = repository();
		tested.add(List.of(bankEntry("Aurora", ONE_MINUTE_BEFORE_BOUNDARY, 100), bankEntry("Aurora", BOUNDARY, 200)));

		tested.getAllFor("Aurora");
		Throwable lockRefusal = catchThrowable(databaseFile::takeAndReleaseTheExclusiveLockWithoutWaiting);

		assertThat(lockRefusal).isNull();
	}

	@Test
	void addStoresSeveralRowsInOneCommit() {
		BankDatabaseRepository tested = repository();
		List<BankEntry> severalRows = List.of(bankEntry("Aurora", BOUNDARY, 1), bankEntry("Aurora", BOUNDARY, 2), bankEntry("Boreas", BOUNDARY, 3));

		int commits = databaseFile.commitsDuring(() -> tested.add(severalRows));

		assertThat(commits).isEqualTo(1);
	}

	@Test
	void addStoresNothingWhenAnEntryCannotBeMapped() {
		BankDatabaseRepository tested = repository();
		List<BankEntry> batch = List.of(bankEntry("Aurora", BOUNDARY, 1), entryWithoutTimestamp(2));

		Throwable failure = catchThrowable(() -> tested.add(batch));
		long stored = tested.countFor("Aurora");

		assertThat(failure).isNotNull();
		assertThat(stored).isZero();
	}

	@Test
	void aRefusedRowStoresAtMostTheRowsBeforeIt() {
		BankDatabaseRepository tested = repository();
		List<BankEntry> batch = List
				.of(bankEntry("Aurora", ONE_MINUTE_BEFORE_BOUNDARY, 1), bankEntry("Aurora", BOUNDARY, 2), entryWithoutAvatar(3), bankEntry("Aurora", ONE_MINUTE_AFTER_BOUNDARY, 4));

		Throwable failure = catchThrowable(() -> tested.add(batch));
		Throwable lockRefusal = catchThrowable(databaseFile::takeAndReleaseTheExclusiveLockWithoutWaiting);
		List<Integer> committed = databaseFile.committedValues(BankDatabaseEntry.TABLE, "amount");

		assertThat(failure).hasStackTraceContaining("NOT NULL constraint failed");
		assertThat(lockRefusal).isNull();
		assertThat(List.of(List.of(), List.of(1), List.of(1, 2))).contains(committed);
	}

	@Test
	void countForCountsOnlyTheRowsOfTheGivenAvatar() {
		BankDatabaseRepository repo = repository();
		repo.add(List.of(bankEntry("Aurora", ONE_MINUTE_BEFORE_BOUNDARY, 100), bankEntry("Aurora", BOUNDARY, 200), bankEntry("Boreas", ONE_MINUTE_AFTER_BOUNDARY, 300)));

		long count = repo.countFor("Aurora");

		assertThat(count).isEqualTo(2);
	}

	@Test
	void countForReturnsZeroForAnAvatarWithoutRows() {
		BankDatabaseRepository repo = repository();

		long count = repo.countFor("Nobody");

		assertThat(count).isZero();
	}

	@Test
	void namesTheLatestTimestampOfEveryAvatarThatHasRows() {
		BankDatabaseRepository repo = repository();
		repo.add(List.of(bankEntry("Aurora", ONE_MINUTE_BEFORE_BOUNDARY, 1), bankEntry("Aurora", ONE_MINUTE_AFTER_BOUNDARY, 2), bankEntry("Boreas", BOUNDARY, 3)));

		Map<String, Instant> latest = repo.latestTimestampPerAvatar();

		assertThat(latest).containsExactlyInAnyOrderEntriesOf(Map.of("Aurora", ONE_MINUTE_AFTER_BOUNDARY, "Boreas", BOUNDARY));
	}

	@Test
	void namesNobodyWhileTheLedgerHasNoRowAtAll() {
		Map<String, Instant> latest = repository().latestTimestampPerAvatar();

		assertThat(latest).isEmpty();
	}

	private static BankEntry bankEntry(String avatar, Instant timeStamp, int amount) {
		return new BankEntry(timeStamp, avatar, amount, TransferType.EINLAGERUNG);
	}

	private static BankEntry entryWithoutTimestamp(int amount) {
		return new BankEntry(null, "Aurora", amount, TransferType.EINLAGERUNG);
	}

	private static BankEntry entryWithoutAvatar(int amount) {
		return new BankEntry(BOUNDARY, null, amount, TransferType.EINLAGERUNG);
	}

	private static Configuration configurationFor(String databasePath) {
		return new Configuration() {
			@Override
			public String getDatabasePath() {
				return databasePath;
			}
		};
	}

	private BankDatabaseRepository repository() {
		return new BankDatabaseRepository(database);
	}
}
