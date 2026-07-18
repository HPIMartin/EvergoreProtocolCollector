package dev.schoenberg.evergore.protocolParser.database.storage;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import dev.schoenberg.evergore.protocolParser.LoggerSpy;
import dev.schoenberg.evergore.protocolParser.businessLogic.base.TransferType;
import dev.schoenberg.evergore.protocolParser.businessLogic.storage.StorageEntry;
import dev.schoenberg.evergore.protocolParser.helper.config.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class StorageDatabaseRepositoryTest {

	@Test
	void getAllSinceIncludesTheRowExactlyAtTheGivenTimestamp() {
		StorageDatabaseRepository repo = StorageDatabaseRepository.get(inMemoryConfiguration(), new LoggerSpy(), () -> {});
		Instant boundary = Instant.parse("2024-06-01T13:37:00Z");
		repo.add(List.of(new StorageEntry(boundary, "Aurora", 3, "Drachenhaut", 80, TransferType.EINLAGERUNG)));

		List<StorageEntry> result = repo.getAllSince(boundary);

		assertThat(result).containsExactly(new StorageEntry(boundary, "Aurora", 3, "Drachenhaut", 80, TransferType.EINLAGERUNG));
	}

	@Test
	void getAllSinceExcludesRowsOlderThanTheGivenTimestamp() {
		StorageDatabaseRepository repo = StorageDatabaseRepository.get(inMemoryConfiguration(), new LoggerSpy(), () -> {});
		Instant boundary = Instant.parse("2024-06-01T13:37:00Z");
		repo.add(List.of(new StorageEntry(Instant.parse("2024-06-01T13:36:00Z"), "Aurora", 1, "Drachenhaut", 80, TransferType.EINLAGERUNG)));

		List<StorageEntry> result = repo.getAllSince(boundary);

		assertThat(result).isEmpty();
	}

	@Test
	void getAllSinceIncludesRowsNewerThanTheGivenTimestamp() {
		StorageDatabaseRepository repo = StorageDatabaseRepository.get(inMemoryConfiguration(), new LoggerSpy(), () -> {});
		Instant boundary = Instant.parse("2024-06-01T13:37:00Z");
		StorageEntry newer = new StorageEntry(Instant.parse("2024-06-01T13:38:00Z"), "Aurora", 3, "Drachenhaut", 80, TransferType.EINLAGERUNG);
		repo.add(List.of(newer));

		List<StorageEntry> result = repo.getAllSince(boundary);

		assertThat(result).containsExactly(newer);
	}

	private static Configuration inMemoryConfiguration() {
		return new Configuration() {
			@Override
			public String getDatabasePath() {
				return ":memory:";
			}
		};
	}
}
