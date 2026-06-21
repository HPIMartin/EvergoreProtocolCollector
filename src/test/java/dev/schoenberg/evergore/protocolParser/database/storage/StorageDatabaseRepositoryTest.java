package dev.schoenberg.evergore.protocolParser.database.storage;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import dev.schoenberg.evergore.protocolParser.LoggerSpy;
import dev.schoenberg.evergore.protocolParser.businessLogic.base.TransferType;
import dev.schoenberg.evergore.protocolParser.businessLogic.storage.StorageEntry;
import dev.schoenberg.evergore.protocolParser.helper.config.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class StorageDatabaseRepositoryTest {

	@Test
	void getNewestReturnsEmptyOnEmptyRepository() {
		StorageDatabaseRepository repo = StorageDatabaseRepository.get(inMemoryConfiguration(), new LoggerSpy(), () -> {});

		Optional<StorageEntry> result = repo.getNewest();

		assertThat(result).isEmpty();
	}

	@Test
	void getNewestReturnsEntryWithLatestTimestampAfterInserts() {
		StorageDatabaseRepository repo = StorageDatabaseRepository.get(inMemoryConfiguration(), new LoggerSpy(), () -> {});
		Instant earlier = Instant.parse("2024-01-01T00:00:00Z");
		Instant later = Instant.parse("2024-06-01T00:00:00Z");
		repo
				.add(List
						.of(new StorageEntry(earlier, "Aurora", 5, "Leinentuch", 100, TransferType.EINLAGERUNG),
								new StorageEntry(later, "Aurora", 3, "Drachenhaut", 80, TransferType.EINLAGERUNG)));

		Optional<StorageEntry> result = repo.getNewest();

		assertThat(result).isPresent();
		assertThat(result.get().timeStamp).isEqualTo(later);
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
