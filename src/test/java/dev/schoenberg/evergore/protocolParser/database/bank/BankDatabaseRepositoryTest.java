package dev.schoenberg.evergore.protocolParser.database.bank;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.schoenberg.evergore.protocolParser.LoggerSpy;
import dev.schoenberg.evergore.protocolParser.businessLogic.banking.BankEntry;
import dev.schoenberg.evergore.protocolParser.businessLogic.base.TransferType;
import dev.schoenberg.evergore.protocolParser.helper.config.Configuration;

import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.silentThrow;
import static org.assertj.core.api.Assertions.assertThat;

class BankDatabaseRepositoryTest {
	private static final String FRESH_DB_PATH = "build/tmp/test/bankRepositoryTest.sqlite";

	@BeforeEach
	void deleteStaleDatabase() {
		silentThrow(() -> Files.deleteIfExists(Paths.get(FRESH_DB_PATH)));
	}

	@Test
	void freshlyConstructedRepositoryIsImmediatelyUsableWithoutSeparateInit() {
		Configuration config = testConfiguration();
		LoggerSpy logger = new LoggerSpy();

		BankDatabaseRepository repo = BankDatabaseRepository.get(config, logger, () -> {});
		BankEntry entry = new BankEntry(Instant.parse("2024-01-10T10:00:00Z"), "Aurora", 1000, TransferType.EINLAGERUNG);
		repo.add(List.of(entry));

		List<BankEntry> found = repo.getAllFor("Aurora", 0, 10);

		assertThat(found).hasSize(1);
		assertThat(found.get(0).avatar).isEqualTo("Aurora");
		assertThat(found.get(0).amount).isEqualTo(1000);
	}

	@Test
	void getNewestReturnsEmptyOnEmptyRepository() {
		BankDatabaseRepository repo = BankDatabaseRepository.get(inMemoryConfiguration(), new LoggerSpy(), () -> {});

		Optional<BankEntry> result = repo.getNewest();

		assertThat(result).isEmpty();
	}

	@Test
	void getNewestReturnsEntryWithLatestTimestampAfterInserts() {
		BankDatabaseRepository repo = BankDatabaseRepository.get(inMemoryConfiguration(), new LoggerSpy(), () -> {});
		Instant earlier = Instant.parse("2024-01-01T00:00:00Z");
		Instant later = Instant.parse("2024-06-01T00:00:00Z");
		repo.add(List.of(new BankEntry(earlier, "Aurora", 100, TransferType.EINLAGERUNG), new BankEntry(later, "Aurora", 200, TransferType.EINLAGERUNG)));

		Optional<BankEntry> result = repo.getNewest();

		assertThat(result).isPresent();
		assertThat(result.get().timeStamp).isEqualTo(later);
	}

	private static Configuration testConfiguration() {
		return new Configuration() {
			@Override
			public String getDatabasePath() {
				return FRESH_DB_PATH;
			}
		};
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
