package dev.schoenberg.evergore.protocolParser.dataExtraction;

import static dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey.*;
import static java.util.Arrays.*;

import java.time.*;
import java.util.*;
import java.util.function.*;

import dev.schoenberg.evergore.protocolParser.*;
import dev.schoenberg.evergore.protocolParser.businessLogic.banking.*;
import dev.schoenberg.evergore.protocolParser.businessLogic.base.TransferType.*;
import dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.*;
import dev.schoenberg.evergore.protocolParser.businessLogic.storage.*;
import jakarta.inject.*;

@Singleton
public class EvergoreDataEvaluator {
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
		LocalDateTime lastUpdated = updatedUpdatedDate();
		updateAvatarInformation(lastUpdated);
	}

	private void updateAvatarInformation(LocalDateTime lastUpdated) {
		Set<String> avatars = new HashSet<>(bankRepo.getAllDifferentAvatars());
		avatars.addAll(storageRepo.getAllDifferentAvatars());

		avatars.forEach(avatar -> updateInformation(avatar, lastUpdated));
	}

	private LocalDateTime updatedUpdatedDate() {
		LocalDateTime lastUpdated = getStoredValue(getLastUpdatedKey(), LocalDateTime.MIN);

		logger.info("Old value: " + lastUpdated);

		LocalDateTime now = LocalDateTime.now();
		MetaInformation<LocalDateTime> newUpdatedInformation = new MetaInformation<>(getLastUpdatedKey(), now);
		metaRepo.add(asList(newUpdatedInformation));

		logger.info("New value: " + getStoredValue(getLastUpdatedKey(), LocalDateTime.MIN));

		return lastUpdated;
	}

	private void updateInformation(String avatar, LocalDateTime lastUpdated) {
		MetaInformationKey<Long> bankPlacementKey = getBankPlacement(avatar);
		MetaInformationKey<Long> bankWithdrawlKey = getBankWithdrawl(avatar);

		long bankPlacement = getStoredValue(bankPlacementKey, 0L);
		long bankWithdrawl = getStoredValue(bankWithdrawlKey, 0L);

		BankStatus bank = new BankStatus(bankPlacement, bankWithdrawl);
		bankRepo.getAllFor(avatar, lastUpdated).forEach(e -> e.type.accept(visitor).accept(bank, e.amount));

		MetaInformation<Long> updatedBankPlacement = new MetaInformation<>(bankPlacementKey, bank.placement);
		MetaInformation<Long> updatedBankWithdrawl = new MetaInformation<>(bankWithdrawlKey, bank.withdrawl);
		metaRepo.add(asList(updatedBankPlacement, updatedBankWithdrawl));
	}

	private static class BankStatus {
		private long placement;
		private long withdrawl;

		public BankStatus(long bankPlacement, long bankWithdrawl) {
			placement = bankPlacement;
			withdrawl = bankWithdrawl;
		}

		public void addPlacement(int value) {
			placement += value;
		}

		public void addWithdarwl(int value) {
			withdrawl += value;
		}
	}

	private final TransferTypeDataEvaluatorVisitor visitor = new TransferTypeDataEvaluatorVisitor();

	private static class TransferTypeDataEvaluatorVisitor implements TransfertTypeVisitor<BiConsumer<BankStatus, Integer>> {
		@Override
		public BiConsumer<BankStatus, Integer> place() {
			return BankStatus::addPlacement;
		}

		@Override
		public BiConsumer<BankStatus, Integer> withdrawl() {
			return BankStatus::addWithdarwl;
		}
	}

	private <T> T getStoredValue(MetaInformationKey<T> key, T alternative) {
		return metaRepo.get(key).orElse(alternative);
	}
}
