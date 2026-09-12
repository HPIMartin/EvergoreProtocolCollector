package dev.schoenberg.evergore.protocolParser.application;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.LongConsumer;
import java.util.function.ToDoubleFunction;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import dev.schoenberg.evergore.protocolParser.Logger;
import dev.schoenberg.evergore.protocolParser.businessLogic.KnownAvatars;
import dev.schoenberg.evergore.protocolParser.businessLogic.banking.BankEntry;
import dev.schoenberg.evergore.protocolParser.businessLogic.banking.BankRepository;
import dev.schoenberg.evergore.protocolParser.businessLogic.base.TransferType.TransferTypeVisitor;
import dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformation;
import dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey;
import dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationRepository;
import dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationSnapshot;
import dev.schoenberg.evergore.protocolParser.businessLogic.storage.StorageEntry;
import dev.schoenberg.evergore.protocolParser.businessLogic.storage.StorageRepository;
import dev.schoenberg.evergore.protocolParser.domain.EvergoreItem;

import static dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey.getBankPlacement;
import static dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey.getBankWithdrawl;
import static dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey.getLastUpdatedKey;
import static dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey.getStorageCraftSubsidy;
import static dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey.getStorageDonation;
import static dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey.getStoragePlacement;
import static dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey.getStorageWithdrawl;
import static dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey.getSumsRecomputedAt;
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
		List<String> failedAvatarNames = new ArrayList<>();
		List<MetaInformation<?>> recomputed = new ArrayList<>();
		MetaInformationSnapshot beforeThisRun = metaRepo.snapshot();
		Instant runInstant = clock.instant();
		List<String> guild = knownAvatars.sortedByName();
		Optional<Instant> runBeforeThisOne = beforeThisRun.lastRecomputeOf(guild);

		guild.forEach(avatar -> collectInformationOf(avatar, runInstant, recomputed, unknownItemNames, failedAvatarNames));
		failedAvatarNames.forEach(avatar -> seedRecomputeInstantOf(avatar, beforeThisRun, runBeforeThisOne, recomputed));
		recomputed.add(new MetaInformation<>(getLastUpdatedKey(), LocalDateTime.ofInstant(runInstant, clock.getZone())));
		metaRepo.add(recomputed);

		return new EvaluationResult(List.copyOf(unknownItemNames), List.copyOf(failedAvatarNames));
	}

	private static void seedRecomputeInstantOf(String avatar, MetaInformationSnapshot beforeThisRun, Optional<Instant> runBeforeThisOne, List<MetaInformation<?>> recomputed) {
		MetaInformationKey<Instant> key = getSumsRecomputedAt(avatar);
		if (beforeThisRun.get(key).isPresent()) {
			return;
		}

		runBeforeThisOne.ifPresent(previousRun -> recomputed.add(new MetaInformation<>(key, previousRun)));
	}

	private void collectInformationOf(String avatar, Instant runInstant, List<MetaInformation<?>> recomputed, List<String> unknownItemNames, List<String> failedAvatarNames) {
		try {
			recomputed.addAll(informationOf(avatar, runInstant, unknownItemNames));
		} catch (RuntimeException failure) {
			logger.error("Unable to recompute the sums of " + avatar + "; keeping the stored ones.", failure);
			failedAvatarNames.add(avatar);
		}
	}

	private List<MetaInformation<?>> informationOf(String avatar, Instant runInstant, List<String> unknownItemNames) {
		List<MetaInformation<?>> information = new ArrayList<>(bankInformationOf(avatar));
		information.addAll(storageInformationOf(avatar, unknownItemNames));
		information.add(new MetaInformation<>(getSumsRecomputedAt(avatar), runInstant));
		return information;
	}

	private List<MetaInformation<Long>> bankInformationOf(String avatar) {
		MetaInformationKey<Long> bankPlacementKey = getBankPlacement(avatar);
		MetaInformationKey<Long> bankWithdrawlKey = getBankWithdrawl(avatar);

		BankStatus bank = new BankStatus(0L, 0L);
		bankRepo.getAllFor(avatar).forEach(e -> e.type().accept(bankVisitor).accept(bank, e));

		return asList(new MetaInformation<>(bankPlacementKey, bank.placement), new MetaInformation<>(bankWithdrawlKey, bank.withdrawl));
	}

	private List<MetaInformation<Double>> storageInformationOf(String avatar, List<String> unknownItemNames) {
		MetaInformationKey<Double> storagePlacementKey = getStoragePlacement(avatar);
		MetaInformationKey<Double> storageWithdrawlKey = getStorageWithdrawl(avatar);
		MetaInformationKey<Double> storageDonationKey = getStorageDonation(avatar);
		MetaInformationKey<Double> storageCraftSubsidyKey = getStorageCraftSubsidy(avatar);

		StorageStatus storage = new StorageStatus();
		storageRepo
				.getAllFor(avatar)
				.stream()
				.map(e -> new StorageEntryItem(e, findItem(e, unknownItemNames)))
				.forEach(e -> e.entry().type().accept(storageEntryVisitor).accept(storage, e));

		return asList(new MetaInformation<>(storagePlacementKey, storage.placement), new MetaInformation<>(storageWithdrawlKey, storage.withdrawl),
				new MetaInformation<>(storageDonationKey, storage.donation), new MetaInformation<>(storageCraftSubsidyKey, storage.craftSubsidy));
	}

	private static class StorageStatus {
		private double placement;
		private double withdrawl;
		private double donation;
		private double craftSubsidy;

		public void addPlacement(double credited, double goodsValueOfTheDeposit) {
			placement += credited;
			donation += Math.max(goodsValueOfTheDeposit - credited, 0);
			craftSubsidy += Math.max(credited - goodsValueOfTheDeposit, 0);
		}

		public void addWithdrawl(double cost) {
			withdrawl += cost;
		}
	}

	private final TransferTypeStorageEntryVisitor storageEntryVisitor = new TransferTypeStorageEntryVisitor();

	private static class TransferTypeStorageEntryVisitor implements TransferTypeVisitor<BiConsumer<StorageStatus, StorageEntryItem>> {
		@Override
		public BiConsumer<StorageStatus, StorageEntryItem> place() {
			return (status, entry) -> status.addPlacement(valueOf(entry, EvergoreItem::getStorageValue), valueOf(entry, EvergoreItem::getWithdrawlValue));
		}

		@Override
		public BiConsumer<StorageStatus, StorageEntryItem> withdrawl() {
			return (status, entry) -> status.addWithdrawl(valueOf(entry, EvergoreItem::getWithdrawlValue));
		}

		private static double valueOf(StorageEntryItem entry, ToDoubleFunction<EvergoreItem> valueFunction) {
			return valueFunction.applyAsDouble(entry.item()) * entry.entry().quantity() * (entry.entry().quality() / 100D);
		}
	}

	private static final String TWO_HANDED_SUFFIX = " [2H]";
	private static final Pattern MAGIC_AFFIX = Pattern.compile(" (?:des|der) \\p{Lu}\\p{L}+( \\[2H\\])?$");

	private EvergoreItem findItem(StorageEntry entry, List<String> unknownItemNames) {
		return spellingsOf(entry.name()).map(EvergoreDataEvaluator::itemNamed).flatMap(Optional::stream).findFirst().orElseGet(() -> {
			logger.warn("Unable to find item: " + entry.name());
			unknownItemNames.add(entry.name());
			return UNDEFINED;
		});
	}

	private static Stream<String> spellingsOf(String ingameName) {
		String plain = withoutMagicAffix(ingameName);
		return Stream.of(ingameName, plain, withTwoHandedSuffixToggled(ingameName), withTwoHandedSuffixToggled(plain)).distinct();
	}

	private static Optional<EvergoreItem> itemNamed(String ingameName) {
		return Arrays.stream(EvergoreItem.values()).filter(item -> item.ingameName.equals(ingameName)).findAny();
	}

	private static String withoutMagicAffix(String ingameName) {
		return MAGIC_AFFIX.matcher(ingameName).replaceFirst("$1");
	}

	private static String withTwoHandedSuffixToggled(String ingameName) {
		return ingameName.endsWith(TWO_HANDED_SUFFIX) ? ingameName.substring(0, ingameName.length() - TWO_HANDED_SUFFIX.length()) : ingameName + TWO_HANDED_SUFFIX;
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
