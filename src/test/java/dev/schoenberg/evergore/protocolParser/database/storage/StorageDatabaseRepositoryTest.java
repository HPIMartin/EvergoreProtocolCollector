package dev.schoenberg.evergore.protocolParser.database.storage;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.schoenberg.evergore.protocolParser.LoggerSpy;
import dev.schoenberg.evergore.protocolParser.businessLogic.base.TransferType;
import dev.schoenberg.evergore.protocolParser.businessLogic.storage.StorageEntry;
import dev.schoenberg.evergore.protocolParser.helper.config.Configuration;

import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.silentThrow;
import static org.assertj.core.api.Assertions.assertThat;

class StorageDatabaseRepositoryTest {
	private static final Instant BOUNDARY = Instant.parse("2024-06-01T13:37:00Z");
	private static final Instant ONE_MINUTE_BEFORE_BOUNDARY = BOUNDARY.minusSeconds(60);
	private static final Instant ONE_MINUTE_AFTER_BOUNDARY = BOUNDARY.plusSeconds(60);
	private static final String FRESH_DB_PATH = "build/tmp/test/storageRepositoryTest.sqlite";

	@BeforeEach
	void deleteStaleDatabase() {
		silentThrow(() -> Files.deleteIfExists(Paths.get(FRESH_DB_PATH)));
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

	private static Configuration configurationFor(String databasePath) {
		return new Configuration() {
			@Override
			public String getDatabasePath() {
				return databasePath;
			}
		};
	}

	private static StorageDatabaseRepository repository() {
		return StorageDatabaseRepository.get(configurationFor(FRESH_DB_PATH), new LoggerSpy(), () -> {});
	}
}
