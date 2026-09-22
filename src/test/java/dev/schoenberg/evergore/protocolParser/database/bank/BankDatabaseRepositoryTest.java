package dev.schoenberg.evergore.protocolParser.database.bank;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.schoenberg.evergore.protocolParser.LoggerSpy;
import dev.schoenberg.evergore.protocolParser.businessLogic.banking.BankEntry;
import dev.schoenberg.evergore.protocolParser.businessLogic.base.TransferType;
import dev.schoenberg.evergore.protocolParser.database.SqliteFile;
import dev.schoenberg.evergore.protocolParser.helper.config.Configuration;

import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.silentThrow;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class BankDatabaseRepositoryTest {
	private static final String FRESH_DB_PATH = "build/tmp/test/bankRepositoryTest.sqlite";
	private static final Instant BOUNDARY = Instant.parse("2024-06-01T13:37:00Z");
	private static final Instant ONE_MINUTE_BEFORE_BOUNDARY = BOUNDARY.minusSeconds(60);
	private static final Instant ONE_MINUTE_AFTER_BOUNDARY = BOUNDARY.plusSeconds(60);

	private final SqliteFile databaseFile = new SqliteFile(FRESH_DB_PATH);

	@BeforeEach
	void deleteStaleDatabase() {
		silentThrow(() -> Files.deleteIfExists(Paths.get(FRESH_DB_PATH)));
	}

	@Test
	void freshlyConstructedRepositoryIsImmediatelyUsableWithoutSeparateInit() {
		BankDatabaseRepository repo = repository();
		BankEntry stored = bankEntry("Aurora", BOUNDARY, 1000);
		repo.add(List.of(stored));

		List<BankEntry> found = repo.getAllFor("Aurora", 0, 10);

		assertThat(found).containsExactly(stored);
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

	private static Configuration configurationFor(String databasePath) {
		return new Configuration() {
			@Override
			public String getDatabasePath() {
				return databasePath;
			}
		};
	}

	private static BankDatabaseRepository repository() {
		return BankDatabaseRepository.get(configurationFor(FRESH_DB_PATH), new LoggerSpy(), () -> {});
	}
}
