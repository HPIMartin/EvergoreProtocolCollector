package dev.schoenberg.evergore.protocolParser.application;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.DoubleConsumer;
import java.util.function.Function;
import java.util.function.LongConsumer;
import java.util.function.ToDoubleFunction;

import dev.schoenberg.evergore.protocolParser.Logger;
import dev.schoenberg.evergore.protocolParser.businessLogic.banking.BankEntry;
import dev.schoenberg.evergore.protocolParser.businessLogic.banking.BankRepository;
import dev.schoenberg.evergore.protocolParser.businessLogic.base.TransferType.TransferTypeVisitor;
import dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformation;
import dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey;
import dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationRepository;
import dev.schoenberg.evergore.protocolParser.businessLogic.storage.StorageEntry;
import dev.schoenberg.evergore.protocolParser.businessLogic.storage.StorageRepository;
import dev.schoenberg.evergore.protocolParser.domain.EvergoreItem;

import static dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey.getBankPlacement;
import static dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey.getBankWithdrawl;
import static dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey.getLastUpdatedKey;
import static dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey.getStoragePlacement;
import static dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey.getStorageWithdrawl;
import static dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.UNDEFINED;
import static java.util.Arrays.asList;

public class EvergoreDataEvaluator {
	private static final LocalDateTime BEGINNING_OF_TIME = LocalDateTime.of(1, 1, 1, 0, 0);

	private final MetaInformationRepository metaRepo;
	private final BankRepository bankRepo;
	private final StorageRepository storageRepo;
	private final Logger logger;

	public EvergoreDataEvaluator(MetaInformationRepository metaRepo, StorageRepository storageRepo, BankRepository bankRepo, Logger logger) {
		this.metaRepo = metaRepo;
		this.bankRepo = bankRepo;
		this.storageRepo = storageRepo;
		this.logger = logger;
	}

	public void evaluateData() {
		LocalDateTime lastUpdated = advanceWatermarkAndReturnPrevious();
		updateAvatarInformation(lastUpdated);
	}

	private void updateAvatarInformation(LocalDateTime lastUpdated) {
		Set<String> avatars = new HashSet<>(bankRepo.getAllDifferentAvatars());
		avatars.addAll(storageRepo.getAllDifferentAvatars());

		avatars.forEach(avatar -> updateInformation(avatar, lastUpdated));
	}

	private LocalDateTime advanceWatermarkAndReturnPrevious() {
		LocalDateTime lastUpdated = getStoredValue(getLastUpdatedKey(), BEGINNING_OF_TIME);

		logger.info("Old value: " + lastUpdated);

		LocalDateTime now = LocalDateTime.now();
		MetaInformation<LocalDateTime> newUpdatedInformation = new MetaInformation<>(getLastUpdatedKey(), now);
		metaRepo.add(asList(newUpdatedInformation));

		logger.info("New value: " + getStoredValue(getLastUpdatedKey(), BEGINNING_OF_TIME));

		return lastUpdated;
	}

	private void updateInformation(String avatar, LocalDateTime lastUpdated) {
		updateBankInformation(avatar, lastUpdated);
		updateStorageInformation(avatar, lastUpdated);
	}

	private void updateBankInformation(String avatar, LocalDateTime lastUpdated) {
		MetaInformationKey<Long> bankPlacementKey = getBankPlacement(avatar);
		MetaInformationKey<Long> bankWithdrawlKey = getBankWithdrawl(avatar);

		long bankPlacement = getStoredValue(bankPlacementKey, 0L);
		long bankWithdrawl = getStoredValue(bankWithdrawlKey, 0L);

		BankStatus bank = new BankStatus(bankPlacement, bankWithdrawl);
		bankRepo.getAllFor(avatar, lastUpdated).forEach(e -> e.type().accept(bankVisitor).accept(bank, e));

		MetaInformation<Long> updatedBankPlacement = new MetaInformation<>(bankPlacementKey, bank.placement);
		MetaInformation<Long> updatedBankWithdrawl = new MetaInformation<>(bankWithdrawlKey, bank.withdrawl);
		metaRepo.add(asList(updatedBankPlacement, updatedBankWithdrawl));
	}

	private void updateStorageInformation(String avatar, LocalDateTime lastUpdated) {
		MetaInformationKey<Double> storagePlacementKey = getStoragePlacement(avatar);
		MetaInformationKey<Double> storageWithdrawlKey = getStorageWithdrawl(avatar);

		double storagePlacement = getStoredValue(storagePlacementKey, 0D);
		double storageWithdrawl = getStoredValue(storageWithdrawlKey, 0D);

		StorageStatus storage = new StorageStatus(storagePlacement, storageWithdrawl);
		storageRepo
				.getAllFor(avatar, lastUpdated)
				.stream()
				.map(e -> new StorageEntryItem(e, findItem(e)))
				.forEach(e -> e.entry().type().accept(storageEntryVisitor).accept(storage, e));

		MetaInformation<Double> updatedStoragePlacement = new MetaInformation<>(storagePlacementKey, storage.placement);
		MetaInformation<Double> updatedStorageWithdrawl = new MetaInformation<>(storageWithdrawlKey, storage.withdrawl);
		metaRepo.add(asList(updatedStoragePlacement, updatedStorageWithdrawl));
	}

	private static class StorageStatus {
		private double placement;
		private double withdrawl;

		public StorageStatus(double storagePlacement, double storageWithdrawl) {
			placement = storagePlacement;
			withdrawl = storageWithdrawl;
		}

		public void addPlacement(double value) {
			placement += value;
		}

		public void addWithdrawl(double value) {
			withdrawl += value;
		}
	}

	private final TransferTypeStorageEntryVisitor storageEntryVisitor = new TransferTypeStorageEntryVisitor();

	private class TransferTypeStorageEntryVisitor implements TransferTypeVisitor<BiConsumer<StorageStatus, StorageEntryItem>> {
		@Override
		public BiConsumer<StorageStatus, StorageEntryItem> place() {
			return operation(s -> s::addPlacement, EvergoreItem::getStorageValue);
		}

		@Override
		public BiConsumer<StorageStatus, StorageEntryItem> withdrawl() {
			return operation(s -> s::addWithdrawl, EvergoreItem::getWithdrawlValue);
		}

		private BiConsumer<StorageStatus, StorageEntryItem> operation(Function<StorageStatus, DoubleConsumer> statusFunction, ToDoubleFunction<EvergoreItem> valueFunction) {
			return (status, value) -> statusFunction.apply(status).accept(valueFunction.applyAsDouble(value.item()) * value.entry().quantity() * (value.entry().quality() / 100D));
		}
	}

	private EvergoreItem findItem(StorageEntry entry) {
		return Arrays.stream(EvergoreItem.values()).filter(e -> e.ingameName.equals(entry.name())).findAny().orElseGet(() -> {
			logger.info("Unable to find item: " + entry.name());
			return UNDEFINED;
		});
	}

	private record StorageEntryItem(StorageEntry entry, EvergoreItem item) {}

	private static class BankStatus {
		private long placement;
		private long withdrawl;

		public BankStatus(long bankPlacement, long bankWithdrawl) {
			placement = bankPlacement;
			withdrawl = bankWithdrawl;
		}

		public void addPlacement(long value) {
			placement += value;
		}

		public void addWithdrawl(long value) {
			withdrawl += value;
		}
	}

	private final TransferTypeBankEntryVisitor bankVisitor = new TransferTypeBankEntryVisitor();

	private static class TransferTypeBankEntryVisitor implements TransferTypeVisitor<BiConsumer<BankStatus, BankEntry>> {
		@Override
		public BiConsumer<BankStatus, BankEntry> place() {
			return operation(s -> s::addPlacement);
		}

		@Override
		public BiConsumer<BankStatus, BankEntry> withdrawl() {
			return operation(s -> s::addWithdrawl);
		}

		private BiConsumer<BankStatus, BankEntry> operation(Function<BankStatus, LongConsumer> statusFunction) {
			return (status, value) -> statusFunction.apply(status).accept(value.amount());
		}
	}

	private <T> T getStoredValue(MetaInformationKey<T> key, T alternative) {
		return metaRepo.get(key).orElse(alternative);
	}
}
