package dev.schoenberg.evergore.protocolParser.application;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.DoubleConsumer;
import java.util.function.Function;
import java.util.function.LongConsumer;
import java.util.function.ToDoubleFunction;

import dev.schoenberg.evergore.protocolParser.Logger;
import dev.schoenberg.evergore.protocolParser.businessLogic.KnownAvatars;
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
	private final MetaInformationRepository metaRepo;
	private final BankRepository bankRepo;
	private final StorageRepository storageRepo;
	private final KnownAvatars knownAvatars;
	private final Clock clock;
	private final Logger logger;

	public EvergoreDataEvaluator(MetaInformationRepository metaRepo, StorageRepository storageRepo, BankRepository bankRepo, KnownAvatars knownAvatars, Clock clock,
			Logger logger) {
		this.metaRepo = metaRepo;
		this.bankRepo = bankRepo;
		this.storageRepo = storageRepo;
		this.knownAvatars = knownAvatars;
		this.clock = clock;
		this.logger = logger;
	}

	public EvaluationResult evaluateData() {
		List<String> unknownItemNames = new ArrayList<>();
		updateAvatarInformation(unknownItemNames);
		metaRepo.add(asList(new MetaInformation<>(getLastUpdatedKey(), LocalDateTime.now(clock))));
		return new EvaluationResult(List.copyOf(unknownItemNames));
	}

	private void updateAvatarInformation(List<String> unknownItemNames) {
		knownAvatars.sortedByName().forEach(avatar -> updateInformation(avatar, unknownItemNames));
	}

	private void updateInformation(String avatar, List<String> unknownItemNames) {
		updateBankInformation(avatar);
		updateStorageInformation(avatar, unknownItemNames);
	}

	private void updateBankInformation(String avatar) {
		MetaInformationKey<Long> bankPlacementKey = getBankPlacement(avatar);
		MetaInformationKey<Long> bankWithdrawlKey = getBankWithdrawl(avatar);

		BankStatus bank = new BankStatus(0L, 0L);
		bankRepo.getAllFor(avatar).forEach(e -> e.type().accept(bankVisitor).accept(bank, e));

		MetaInformation<Long> updatedBankPlacement = new MetaInformation<>(bankPlacementKey, bank.placement);
		MetaInformation<Long> updatedBankWithdrawl = new MetaInformation<>(bankWithdrawlKey, bank.withdrawl);
		metaRepo.add(asList(updatedBankPlacement, updatedBankWithdrawl));
	}

	private void updateStorageInformation(String avatar, List<String> unknownItemNames) {
		MetaInformationKey<Double> storagePlacementKey = getStoragePlacement(avatar);
		MetaInformationKey<Double> storageWithdrawlKey = getStorageWithdrawl(avatar);

		StorageStatus storage = new StorageStatus(0D, 0D);
		storageRepo
				.getAllFor(avatar)
				.stream()
				.map(e -> new StorageEntryItem(e, findItem(e, unknownItemNames)))
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

	private EvergoreItem findItem(StorageEntry entry, List<String> unknownItemNames) {
		return Arrays.stream(EvergoreItem.values()).filter(e -> e.ingameName.equals(entry.name())).findAny().orElseGet(() -> {
			logger.warn("Unable to find item: " + entry.name());
			unknownItemNames.add(entry.name());
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
}
