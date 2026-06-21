package dev.schoenberg.evergore.protocolParser.dataExtraction;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.schoenberg.evergore.protocolParser.LoggerSpy;
import dev.schoenberg.evergore.protocolParser.businessLogic.banking.BankEntry;
import dev.schoenberg.evergore.protocolParser.businessLogic.banking.BankRepositoryStub;
import dev.schoenberg.evergore.protocolParser.businessLogic.storage.StorageEntry;
import dev.schoenberg.evergore.protocolParser.businessLogic.storage.StorageRepositoryStub;

import static dev.schoenberg.evergore.protocolParser.businessLogic.Constants.APP_ZONE;
import static dev.schoenberg.evergore.protocolParser.businessLogic.base.TransferType.EINLAGERUNG;
import static org.assertj.core.api.Assertions.assertThat;

class EvergoreDataExtractorTest {

	private static final List<String> BANK_LINES = List.of("11.12.2001 13:37 TestAvatar Einlagerung", "100 Gold");

	private static final List<String> LAGER_LINES = List.of("11.12.2001 13:37 TestAvatar Einlagerung", "5 Drachenhaut");

	private CapturingBankRepository bankRepo;
	private CapturingStorageRepository storageRepo;
	private EvergoreDataExtractor tested;

	@BeforeEach
	void setup() {
		bankRepo = new CapturingBankRepository();
		storageRepo = new CapturingStorageRepository();
		tested = new EvergoreDataExtractor(new FakePageSource(), bankRepo, storageRepo, new LoggerSpy());
	}

	@Test
	void parsedBankEntriesArePersistedViaRepository() {
		tested.loadData();

		assertThat(bankRepo.added).hasSize(1);
		assertThat(bankRepo.added.get(0).avatar).isEqualTo("TestAvatar");
		assertThat(bankRepo.added.get(0).type).isEqualTo(EINLAGERUNG);
	}

	@Test
	void parsedStorageEntriesArePersistedViaRepository() {
		tested.loadData();

		assertThat(storageRepo.added).hasSize(1);
		assertThat(storageRepo.added.get(0).avatar).isEqualTo("TestAvatar");
		assertThat(storageRepo.added.get(0).name).isEqualTo("Drachenhaut");
		assertThat(storageRepo.added.get(0).type).isEqualTo(EINLAGERUNG);
	}

	@Test
	void entriesNotNewerThanTheStoredWatermarkAreNotPersisted() {
		Instant afterAllEntries = Instant.parse("2099-01-01T00:00:00Z");
		bankRepo.newest = Optional.of(new BankEntry(afterAllEntries, "", 0, EINLAGERUNG));
		storageRepo.newest = Optional.of(new StorageEntry(afterAllEntries, "", 0, "", 0, EINLAGERUNG));

		tested.loadData();

		assertThat(bankRepo.added).isEmpty();
		assertThat(storageRepo.added).isEmpty();
	}

	@Test
	void allEntriesArePersistedWhenNoNewestEntryExists() {
		bankRepo.newest = Optional.empty();
		storageRepo.newest = Optional.empty();

		tested.loadData();

		assertThat(bankRepo.added).hasSize(1);
		assertThat(storageRepo.added).hasSize(1);
	}

	@Test
	void entriesWithTimestampEqualToTheStoredWatermarkAreNotPersisted() {
		Instant sameInstantAsParsedEntries = LocalDateTime.of(2001, 12, 11, 13, 37).atZone(APP_ZONE).toInstant();
		bankRepo.newest = Optional.of(new BankEntry(sameInstantAsParsedEntries, "", 0, EINLAGERUNG));
		storageRepo.newest = Optional.of(new StorageEntry(sameInstantAsParsedEntries, "", 0, "", 0, EINLAGERUNG));

		tested.loadData();

		assertThat(bankRepo.added).isEmpty();
		assertThat(storageRepo.added).isEmpty();
	}

	private static class FakePageSource implements PageSource {
		@Override
		public PageContents load() {
			return new PageContents(LAGER_LINES, BANK_LINES);
		}
	}

	private static class CapturingBankRepository extends BankRepositoryStub {
		final List<BankEntry> added = new ArrayList<>();
		Optional<BankEntry> newest = Optional.of(new BankEntry(Instant.MIN, "", 0, EINLAGERUNG));

		@Override
		public void add(List<BankEntry> newEntries) {
			added.addAll(newEntries);
		}

		@Override
		public Optional<BankEntry> getNewest() {
			return newest;
		}
	}

	private static class CapturingStorageRepository extends StorageRepositoryStub {
		final List<StorageEntry> added = new ArrayList<>();
		Optional<StorageEntry> newest = Optional.of(new StorageEntry(Instant.MIN, "", 0, "", 0, EINLAGERUNG));

		@Override
		public void add(List<StorageEntry> newEntries) {
			added.addAll(newEntries);
		}

		@Override
		public Optional<StorageEntry> getNewest() {
			return newest;
		}
	}
}
