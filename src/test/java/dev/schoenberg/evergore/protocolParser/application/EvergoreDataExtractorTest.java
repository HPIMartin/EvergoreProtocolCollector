package dev.schoenberg.evergore.protocolParser.application;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.schoenberg.evergore.protocolParser.LoggerSpy;
import dev.schoenberg.evergore.protocolParser.businessLogic.banking.BankEntry;
import dev.schoenberg.evergore.protocolParser.businessLogic.banking.BankRepositoryStub;
import dev.schoenberg.evergore.protocolParser.businessLogic.storage.StorageEntry;
import dev.schoenberg.evergore.protocolParser.businessLogic.storage.StorageRepositoryStub;
import dev.schoenberg.evergore.protocolParser.dataExtraction.PageContents;
import dev.schoenberg.evergore.protocolParser.dataExtraction.PageSource;

import static dev.schoenberg.evergore.protocolParser.businessLogic.Constants.APP_ZONE;
import static dev.schoenberg.evergore.protocolParser.businessLogic.base.TransferType.EINLAGERUNG;
import static org.assertj.core.api.Assertions.assertThat;

class EvergoreDataExtractorTest {

	private static final List<String> BANK_LINES = List.of("11.12.2001 13:37 TestAvatar Einlagerung", "100 Gold");

	private static final List<String> LAGER_LINES = List.of("11.12.2001 13:37 TestAvatar Einlagerung", "5 Drachenhaut");

	private static final Instant BOUNDARY_TIMESTAMP = LocalDateTime.of(2001, 12, 11, 13, 37).atZone(APP_ZONE).toInstant();

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
		assertThat(bankRepo.added.get(0).avatar()).isEqualTo("TestAvatar");
		assertThat(bankRepo.added.get(0).type()).isEqualTo(EINLAGERUNG);
	}

	@Test
	void parsedStorageEntriesArePersistedViaRepository() {
		tested.loadData();

		assertThat(storageRepo.added).hasSize(1);
		assertThat(storageRepo.added.get(0).avatar()).isEqualTo("TestAvatar");
		assertThat(storageRepo.added.get(0).name()).isEqualTo("Drachenhaut");
		assertThat(storageRepo.added.get(0).type()).isEqualTo(EINLAGERUNG);
	}

	// The healing case: a row is still visible in the scraped protocol but is missing from the
	// database (an earlier, buggy scrape dropped it), even though a newer row is already stored.
	@Test
	void ingestsAStillVisibleEntryOlderThanTheStoredMaxThatIsMissingFromTheDatabase() {
		Instant storedMaxTimestamp = LocalDateTime.of(2001, 12, 15, 13, 37).atZone(APP_ZONE).toInstant();
		BankEntry storedMaxEntry = new BankEntry(storedMaxTimestamp, "SomeoneElse", 100, EINLAGERUNG);
		bankRepo.expectedSinceArgument = BOUNDARY_TIMESTAMP;
		bankRepo.storedSince = List.of(storedMaxEntry);

		tested.loadData();

		assertThat(bankRepo.added).containsExactly(new BankEntry(BOUNDARY_TIMESTAMP, "TestAvatar", 100, EINLAGERUNG));
	}

	@Test
	void doesNotDuplicateAnIdenticalAlreadyStoredRow() {
		BankEntry sameAsParsedEntry = new BankEntry(BOUNDARY_TIMESTAMP, "TestAvatar", 100, EINLAGERUNG);
		bankRepo.expectedSinceArgument = BOUNDARY_TIMESTAMP;
		bankRepo.storedSince = List.of(sameAsParsedEntry);

		tested.loadData();

		assertThat(bankRepo.added).isEmpty();
	}

	@Test
	void doesNotDuplicateAnIdenticalAlreadyStoredStorageRow() {
		StorageEntry sameAsParsedEntry = new StorageEntry(BOUNDARY_TIMESTAMP, "TestAvatar", 5, "Drachenhaut", 100, EINLAGERUNG);
		storageRepo.expectedSinceArgument = BOUNDARY_TIMESTAMP;
		storageRepo.storedSince = List.of(sameAsParsedEntry);

		tested.loadData();

		assertThat(storageRepo.added).isEmpty();
	}

	@Test
	void ingestsOnlyTheSurplusWhenAnIdenticalPairHasOneAlreadyStored() {
		tested = new EvergoreDataExtractor(new DuplicateBankLinePageSource(), bankRepo, storageRepo, new LoggerSpy());
		BankEntry alreadyStored = new BankEntry(BOUNDARY_TIMESTAMP, "TestAvatar", 100, EINLAGERUNG);
		bankRepo.expectedSinceArgument = BOUNDARY_TIMESTAMP;
		bankRepo.storedSince = List.of(alreadyStored);

		tested.loadData();

		assertThat(bankRepo.added).containsExactly(new BankEntry(BOUNDARY_TIMESTAMP, "TestAvatar", 100, EINLAGERUNG));
	}

	// Two identical entries at the same minute are legitimate (occurrence counting, not set semantics).
	@Test
	void ingestsBothIdenticalScrapedRowsWhenNeitherIsStoredYet() {
		tested = new EvergoreDataExtractor(new DuplicateBankLinePageSource(), bankRepo, storageRepo, new LoggerSpy());

		tested.loadData();

		assertThat(bankRepo.added)
				.containsExactly(new BankEntry(BOUNDARY_TIMESTAMP, "TestAvatar", 100, EINLAGERUNG), new BankEntry(BOUNDARY_TIMESTAMP, "TestAvatar", 100, EINLAGERUNG));
	}

	@Test // page source: newest entry first, as scraped
	void ingestsOldestFirstSoAPartiallyCommittedBatchNeverStrandsOlderEntries() {
		tested = new EvergoreDataExtractor(new NewestFirstPageSource(), bankRepo, storageRepo, new LoggerSpy());

		tested.loadData();

		Instant newerTimestamp = LocalDateTime.of(2001, 12, 12, 13, 37).atZone(APP_ZONE).toInstant();
		assertThat(bankRepo.added)
				.containsExactly(new BankEntry(BOUNDARY_TIMESTAMP, "TestAvatar", 100, EINLAGERUNG), new BankEntry(newerTimestamp, "TestAvatar", 100, EINLAGERUNG));
	}

	@Test // the dedup window must start at the OLDEST scraped timestamp, or older still-visible rows re-ingest forever
	void usesTheOldestScrapedTimestampAsTheDedupWindowBoundary() {
		tested = new EvergoreDataExtractor(new NewestFirstPageSource(), bankRepo, storageRepo, new LoggerSpy());
		BankEntry olderAlreadyStored = new BankEntry(BOUNDARY_TIMESTAMP, "TestAvatar", 100, EINLAGERUNG);
		bankRepo.expectedSinceArgument = BOUNDARY_TIMESTAMP;
		bankRepo.storedSince = List.of(olderAlreadyStored);

		tested.loadData();

		Instant newerTimestamp = LocalDateTime.of(2001, 12, 12, 13, 37).atZone(APP_ZONE).toInstant();
		assertThat(bankRepo.added).containsExactly(new BankEntry(newerTimestamp, "TestAvatar", 100, EINLAGERUNG));
	}

	private static class NewestFirstPageSource implements PageSource {
		@Override
		public PageContents load() {
			return new PageContents(List.of(), List.of("12.12.2001 13:37 TestAvatar Einzahlung", "100 Gold", "11.12.2001 13:37 TestAvatar Einzahlung", "100 Gold"));
		}
	}

	private static class FakePageSource implements PageSource {
		@Override
		public PageContents load() {
			return new PageContents(LAGER_LINES, BANK_LINES);
		}
	}

	private static class DuplicateBankLinePageSource implements PageSource {
		@Override
		public PageContents load() {
			List<String> duplicatedBankLines = List.of("11.12.2001 13:37 TestAvatar Einlagerung", "100 Gold", "11.12.2001 13:37 TestAvatar Einlagerung", "100 Gold");
			return new PageContents(List.of(), duplicatedBankLines);
		}
	}

	private static class CapturingBankRepository extends BankRepositoryStub {
		final List<BankEntry> added = new ArrayList<>();
		Instant expectedSinceArgument;
		List<BankEntry> storedSince = List.of();

		@Override
		public void add(List<BankEntry> newEntries) {
			added.addAll(newEntries);
		}

		// A mutant that passes the wrong argument (e.g. Instant.EPOCH) gets an empty result here,
		// which makes the dedup tests fail instead of silently passing.
		@Override
		public List<BankEntry> getAllSince(Instant timestampInclusive) {
			return timestampInclusive.equals(expectedSinceArgument) ? storedSince : List.of();
		}
	}

	private static class CapturingStorageRepository extends StorageRepositoryStub {
		final List<StorageEntry> added = new ArrayList<>();
		Instant expectedSinceArgument;
		List<StorageEntry> storedSince = List.of();

		@Override
		public void add(List<StorageEntry> newEntries) {
			added.addAll(newEntries);
		}

		// A mutant that passes the wrong argument (e.g. Instant.EPOCH) gets an empty result here,
		// which makes the dedup tests fail instead of silently passing.
		@Override
		public List<StorageEntry> getAllSince(Instant timestampInclusive) {
			return timestampInclusive.equals(expectedSinceArgument) ? storedSince : List.of();
		}
	}
}
