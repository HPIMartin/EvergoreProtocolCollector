package dev.schoenberg.evergore.protocolParser.dataExtraction;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.schoenberg.evergore.protocolParser.LoggerSpy;
import dev.schoenberg.evergore.protocolParser.businessLogic.banking.BankEntry;
import dev.schoenberg.evergore.protocolParser.businessLogic.banking.BankRepositoryStub;
import dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.FakeMetaInformationRepository;
import dev.schoenberg.evergore.protocolParser.businessLogic.storage.StorageEntry;
import dev.schoenberg.evergore.protocolParser.businessLogic.storage.StorageRepositoryStub;

import static dev.schoenberg.evergore.protocolParser.businessLogic.base.TransferType.EINLAGERUNG;
import static dev.schoenberg.evergore.protocolParser.businessLogic.base.TransferType.ENTNAHME;
import static dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey.getBankPlacement;
import static dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey.getBankWithdrawl;
import static dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey.getLastUpdatedKey;
import static dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey.getStoragePlacement;
import static dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey.getStorageWithdrawl;
import static dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.LEINENTUCH;
import static org.assertj.core.api.Assertions.assertThat;

class EvergoreDataEvaluatorTest {

	private static final String AVATAR = "avatar_a";
	private static final String BANK_ONLY_AVATAR = "bank_only";
	private static final String STORAGE_ONLY_AVATAR = "storage_only";

	private FakeMetaInformationRepository metaRepo;
	private BankRepositoryStub bankRepo;
	private StorageRepositoryStub storageRepo;
	private LoggerSpy logger;
	private EvergoreDataEvaluator tested;

	@BeforeEach
	void setup() {
		metaRepo = new FakeMetaInformationRepository();
		bankRepo = new BankRepositoryStub();
		storageRepo = new StorageRepositoryStub();
		logger = new LoggerSpy();
		tested = new EvergoreDataEvaluator(metaRepo, storageRepo, bankRepo, logger);
	}

	@Test
	void aggregatesBankPlacementAndWithdrawlForOneAvatar() {
		bankRepo.seedEntries(AVATAR, List.of(bankPlacement(100), bankPlacement(200), bankWithdrawl(50)));
		bankRepo.seedAvatars(List.of(AVATAR));
		storageRepo.seedAvatars(List.of());

		tested.evaluateData();

		assertThat(metaRepo.<Long>get(getBankPlacement(AVATAR))).contains(300L);
		assertThat(metaRepo.<Long>get(getBankWithdrawl(AVATAR))).contains(50L);
	}

	@Test
	void valuatesStoragePlacementAndWithdrawlForCraftableItemWithPartialQuality() {
		int quantity = 3;
		int quality = 50;
		storageRepo.seedEntries(AVATAR, List.of(storagePlacement(LEINENTUCH.ingameName, quantity, quality), storageWithdrawl(LEINENTUCH.ingameName, quantity, quality)));
		storageRepo.seedAvatars(List.of(AVATAR));
		bankRepo.seedAvatars(List.of());

		tested.evaluateData();

		double expectedPlacement = LEINENTUCH.getStorageValue() * quantity * (quality / 100D);
		double expectedWithdrawl = LEINENTUCH.getWithdrawlValue() * quantity * (quality / 100D);
		assertThat(metaRepo.<Double>get(getStoragePlacement(AVATAR))).contains(expectedPlacement);
		assertThat(metaRepo.<Double>get(getStorageWithdrawl(AVATAR))).contains(expectedWithdrawl);
	}

	@Test
	void unknownItemFallsBackToZeroValueAndLogsMessage() {
		String unknownItemName = "Unobtainium";
		storageRepo.seedEntries(AVATAR, List.of(storagePlacement(unknownItemName, 1, 100)));
		storageRepo.seedAvatars(List.of(AVATAR));
		bankRepo.seedAvatars(List.of());

		tested.evaluateData();

		assertThat(metaRepo.<Double>get(getStoragePlacement(AVATAR))).contains(0.0);
		assertThat(logger.infoMessages()).contains("Unable to find item: " + unknownItemName);
	}

	@Test
	void accumulatesStorageValueOnTopOfPreviouslyStoredValue() {
		double previouslyStored = 100.0;
		metaRepo.put(getStoragePlacement(AVATAR), previouslyStored);
		storageRepo.seedEntries(AVATAR, List.of(storagePlacement(LEINENTUCH.ingameName, 1, 100)));
		storageRepo.seedAvatars(List.of(AVATAR));
		bankRepo.seedAvatars(List.of());

		tested.evaluateData();

		double expected = previouslyStored + LEINENTUCH.getStorageValue() * 1 * 1.0;
		assertThat(metaRepo.<Double>get(getStoragePlacement(AVATAR))).contains(expected);
	}

	@Test
	void processesAvatarsFromBothReposInUnionWritingAllKeysForEachAvatar() {
		bankRepo.seedEntries(BANK_ONLY_AVATAR, List.of(bankPlacement(10)));
		bankRepo.seedAvatars(List.of(BANK_ONLY_AVATAR));
		storageRepo.seedEntries(STORAGE_ONLY_AVATAR, List.of(storagePlacement(LEINENTUCH.ingameName, 1, 100)));
		storageRepo.seedAvatars(List.of(STORAGE_ONLY_AVATAR));

		tested.evaluateData();

		assertThat(metaRepo.<Long>get(getBankPlacement(BANK_ONLY_AVATAR))).contains(10L);
		assertThat(metaRepo.<Double>get(getStoragePlacement(BANK_ONLY_AVATAR))).contains(0.0);
		assertThat(metaRepo.<Double>get(getStoragePlacement(STORAGE_ONLY_AVATAR))).contains(LEINENTUCH.getStorageValue() * 1 * 1.0);
		assertThat(metaRepo.<Long>get(getBankPlacement(STORAGE_ONLY_AVATAR))).contains(0L);
	}

	@Test
	void usesStoredWatermarkAsCutoffForBothReposAndAdvancesItAfterEvaluation() {
		LocalDateTime priorWatermark = LocalDateTime.of(2024, 1, 1, 12, 0);
		metaRepo.put(getLastUpdatedKey(), priorWatermark);
		bankRepo.seedAvatars(List.of(AVATAR));
		storageRepo.seedAvatars(List.of(AVATAR));

		tested.evaluateData();

		assertThat(bankRepo.capturedAfter()).isEqualTo(priorWatermark);
		assertThat(storageRepo.capturedAfter()).isEqualTo(priorWatermark);
		assertThat(metaRepo.<LocalDateTime>get(getLastUpdatedKey())).isPresent().hasValueSatisfying(v -> assertThat(v).isAfter(priorWatermark));
	}

	// --- Test DSL helpers ---

	private static BankEntry bankPlacement(int amount) {
		return new BankEntry(Instant.EPOCH, AVATAR, amount, EINLAGERUNG);
	}

	private static BankEntry bankWithdrawl(int amount) {
		return new BankEntry(Instant.EPOCH, AVATAR, amount, ENTNAHME);
	}

	private static StorageEntry storagePlacement(String itemName, int quantity, int quality) {
		return new StorageEntry(Instant.EPOCH, AVATAR, quantity, itemName, quality, EINLAGERUNG);
	}

	private static StorageEntry storageWithdrawl(String itemName, int quantity, int quality) {
		return new StorageEntry(Instant.EPOCH, AVATAR, quantity, itemName, quality, ENTNAHME);
	}
}
