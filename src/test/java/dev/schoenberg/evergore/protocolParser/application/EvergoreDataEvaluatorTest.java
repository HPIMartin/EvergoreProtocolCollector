package dev.schoenberg.evergore.protocolParser.application;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import dev.schoenberg.evergore.protocolParser.ApplicationFactory;
import dev.schoenberg.evergore.protocolParser.LoggerSpy;
import dev.schoenberg.evergore.protocolParser.businessLogic.KnownAvatars;
import dev.schoenberg.evergore.protocolParser.businessLogic.banking.BankEntry;
import dev.schoenberg.evergore.protocolParser.businessLogic.banking.BankRepositoryStub;
import dev.schoenberg.evergore.protocolParser.businessLogic.contribution.AvatarContribution;
import dev.schoenberg.evergore.protocolParser.businessLogic.contribution.AvatarContributions;
import dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.FakeMetaInformationRepository;
import dev.schoenberg.evergore.protocolParser.businessLogic.storage.StorageEntry;
import dev.schoenberg.evergore.protocolParser.businessLogic.storage.StorageRepositoryStub;
import dev.schoenberg.evergore.protocolParser.domain.EvergoreItem;

import static dev.schoenberg.evergore.protocolParser.businessLogic.Constants.APP_ZONE;
import static dev.schoenberg.evergore.protocolParser.businessLogic.base.TransferType.EINLAGERUNG;
import static dev.schoenberg.evergore.protocolParser.businessLogic.base.TransferType.ENTNAHME;
import static dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey.getBankPlacement;
import static dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey.getBankWithdrawl;
import static dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey.getLastUpdatedKey;
import static dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey.getStorageCraftSubsidy;
import static dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey.getStorageDonation;
import static dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey.getStoragePlacement;
import static dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey.getStorageWithdrawl;
import static dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey.getSumsRecomputedAt;
import static dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.ERDE_EIBENLANZE;
import static dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.LEINENTUCH;
import static dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.MAGIESPLITTER;
import static dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.STERNENSTAUB;
import static java.util.Arrays.stream;
import static org.assertj.core.api.Assertions.assertThat;

class EvergoreDataEvaluatorTest {

	private static final String AVATAR = "avatar_a";
	private static final String BANK_ONLY_AVATAR = "bank_only";
	private static final String STORAGE_ONLY_AVATAR = "storage_only";
	private static final String UNREADABLE_AVATAR = "zzz_unreadable";
	private static final Instant FIXED_NOW = Instant.parse("2026-06-21T12:00:00Z");

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
		tested = new EvergoreDataEvaluator(metaRepo, storageRepo, bankRepo, new KnownAvatars(bankRepo, storageRepo), Clock.fixed(FIXED_NOW, ZoneOffset.UTC), logger);
	}

	@Test
	void keepsRefreshingEveryHealthyAvatarWhileOneAvatarsLedgerCannotBeRead() {
		bankRepo.seedEntries(AVATAR, List.of(bankPlacement(100)));
		bankRepo.seedAvatars(List.of(AVATAR));
		storageRepo.seedAvatars(List.of(UNREADABLE_AVATAR));
		storageRepo.failOn(UNREADABLE_AVATAR);

		EvaluationResult result = tested.evaluateData();

		assertThat(metaRepo.<Long>get(getBankPlacement(AVATAR))).contains(100L);
		assertThat(result.failedAvatarNames()).containsExactly(UNREADABLE_AVATAR);
	}

	@Test
	void leavesTheStoredSumsOfAnAvatarWhoseLedgerCannotBeReadUntouched() {
		metaRepo.put(getBankPlacement(UNREADABLE_AVATAR), 4200L);
		bankRepo.seedAvatars(List.of(UNREADABLE_AVATAR));
		storageRepo.seedAvatars(List.of());
		bankRepo.failOn(UNREADABLE_AVATAR);

		tested.evaluateData();

		assertThat(metaRepo.<Long>get(getBankPlacement(UNREADABLE_AVATAR))).contains(4200L);
	}

	@Test
	void reportsAnAvatarWhoseLedgerCannotBeReadAsAnError() {
		bankRepo.seedAvatars(List.of(UNREADABLE_AVATAR));
		storageRepo.seedAvatars(List.of());
		bankRepo.failOn(UNREADABLE_AVATAR);

		tested.evaluateData();

		assertThat(logger.errorMessages()).anySatisfy(message -> assertThat(message).contains(UNREADABLE_AVATAR));
	}

	@Test
	void writesEveryAvatarsSumsAndTheRunTimestampAsOneBatch() {
		bankRepo.seedEntries(AVATAR, List.of(bankPlacement(100)));
		bankRepo.seedEntries(BANK_ONLY_AVATAR, List.of(bankPlacement(7)));
		bankRepo.seedAvatars(List.of(AVATAR, BANK_ONLY_AVATAR));
		storageRepo.seedAvatars(List.of(STORAGE_ONLY_AVATAR));

		tested.evaluateData();

		assertThat(metaRepo.writtenBatches()).hasSize(1);
		assertThat(metaRepo.writtenBatches().get(0))
				.containsExactlyInAnyOrder(getBankPlacement(AVATAR).id, getBankWithdrawl(AVATAR).id, getStoragePlacement(AVATAR).id, getStorageWithdrawl(AVATAR).id,
						getSumsRecomputedAt(AVATAR).id, getBankPlacement(BANK_ONLY_AVATAR).id, getBankWithdrawl(BANK_ONLY_AVATAR).id, getStoragePlacement(BANK_ONLY_AVATAR).id,
						getStorageWithdrawl(BANK_ONLY_AVATAR).id, getSumsRecomputedAt(BANK_ONLY_AVATAR).id, getBankPlacement(STORAGE_ONLY_AVATAR).id,
						getBankWithdrawl(STORAGE_ONLY_AVATAR).id, getStoragePlacement(STORAGE_ONLY_AVATAR).id, getStorageWithdrawl(STORAGE_ONLY_AVATAR).id,
						getSumsRecomputedAt(STORAGE_ONLY_AVATAR).id, getStorageDonation(AVATAR).id, getStorageDonation(BANK_ONLY_AVATAR).id,
						getStorageDonation(STORAGE_ONLY_AVATAR).id, getStorageCraftSubsidy(AVATAR).id, getStorageCraftSubsidy(BANK_ONLY_AVATAR).id,
						getStorageCraftSubsidy(STORAGE_ONLY_AVATAR).id, getLastUpdatedKey().id);
	}

	@Test
	void stampsTheRecomputeInstantOfEveryRefreshedAvatar() {
		bankRepo.seedEntries(AVATAR, List.of(bankPlacement(100)));
		bankRepo.seedAvatars(List.of(AVATAR));
		storageRepo.seedAvatars(List.of());

		tested.evaluateData();

		assertThat(metaRepo.<Instant>get(getSumsRecomputedAt(AVATAR))).contains(FIXED_NOW);
	}

	@Test
	void stampsOneAndTheSameRecomputeInstantForEveryAvatarOfARunWhileTheClockKeepsTicking() {
		bankRepo.seedAvatars(List.of(AVATAR, BANK_ONLY_AVATAR));
		storageRepo.seedAvatars(List.of(STORAGE_ONLY_AVATAR));
		EvergoreDataEvaluator ticking = new EvergoreDataEvaluator(metaRepo, storageRepo, bankRepo, new KnownAvatars(bankRepo, storageRepo), new TickingClock(), logger);

		ticking.evaluateData();

		assertThat(List
				.of(metaRepo.<Instant>get(getSumsRecomputedAt(AVATAR)), metaRepo.<Instant>get(getSumsRecomputedAt(BANK_ONLY_AVATAR)),
						metaRepo.<Instant>get(getSumsRecomputedAt(STORAGE_ONLY_AVATAR))))
				.containsOnly(Optional.of(FIXED_NOW));
	}

	@Test
	void leavesTheRecomputeInstantOfAnAvatarWhoseLedgerCannotBeReadAtItsPreviousValue() {
		Instant previousRun = FIXED_NOW.minusSeconds(86400);
		metaRepo.put(getSumsRecomputedAt(UNREADABLE_AVATAR), previousRun);
		bankRepo.seedAvatars(List.of(UNREADABLE_AVATAR));
		storageRepo.seedAvatars(List.of());
		bankRepo.failOn(UNREADABLE_AVATAR);

		tested.evaluateData();

		assertThat(metaRepo.<Instant>get(getSumsRecomputedAt(UNREADABLE_AVATAR))).contains(previousRun);
	}

	@Test
	void seedsTheRecomputeInstantOfAFailedAvatarWithoutOneFromTheRunThatLastReachedTheGuild() {
		Instant runBeforeThisOne = FIXED_NOW.minusSeconds(86400);
		metaRepo.put(getSumsRecomputedAt(AVATAR), runBeforeThisOne);
		bankRepo.seedAvatars(List.of(AVATAR, UNREADABLE_AVATAR));
		storageRepo.seedAvatars(List.of());
		bankRepo.failOn(AVATAR);
		bankRepo.failOn(UNREADABLE_AVATAR);

		tested.evaluateData();

		assertThat(metaRepo.<Instant>get(getSumsRecomputedAt(UNREADABLE_AVATAR))).contains(runBeforeThisOne);
	}

	@Test
	void seedsAcrossTheSecondPassOfTheBerlinFallBackHourWithoutLosingThatHour() {
		Instant secondPassOfTheFallBack = Instant.parse("2025-10-26T01:30:00Z");
		metaRepo.put(getSumsRecomputedAt(AVATAR), secondPassOfTheFallBack);
		metaRepo.put(getLastUpdatedKey(), LocalDateTime.ofInstant(secondPassOfTheFallBack, APP_ZONE));
		bankRepo.seedAvatars(List.of(AVATAR, UNREADABLE_AVATAR));
		storageRepo.seedAvatars(List.of());
		bankRepo.failOn(AVATAR);
		bankRepo.failOn(UNREADABLE_AVATAR);

		tested.evaluateData();

		assertThat(metaRepo.<Instant>get(getSumsRecomputedAt(UNREADABLE_AVATAR))).contains(secondPassOfTheFallBack);
	}

	@Test
	void leavesASeededAvatarAsCurrentAsTheAvatarWhoseStampAnchorsTheLastCollection() {
		Instant lastCollection = Instant.parse("2025-10-26T01:30:00Z");
		metaRepo.put(getSumsRecomputedAt(AVATAR), lastCollection);
		metaRepo.put(getLastUpdatedKey(), LocalDateTime.ofInstant(lastCollection, APP_ZONE));
		bankRepo.seedAvatars(List.of(AVATAR, UNREADABLE_AVATAR));
		storageRepo.seedAvatars(List.of());
		bankRepo.failOn(AVATAR);
		bankRepo.failOn(UNREADABLE_AVATAR);
		tested.evaluateData();

		List<AvatarContribution> guild = new AvatarContributions(new KnownAvatars(bankRepo, storageRepo), metaRepo, bankRepo, storageRepo).ofEveryKnownAvatar().avatars();

		assertThat(guild).extracting(AvatarContribution::staleSumsFrom).containsOnlyNulls();
	}

	@Test
	void leavesAFailedAvatarWithoutARecomputeInstantWhileNoAvatarCarriesOne() {
		bankRepo.seedAvatars(List.of(UNREADABLE_AVATAR));
		storageRepo.seedAvatars(List.of());
		bankRepo.failOn(UNREADABLE_AVATAR);

		tested.evaluateData();

		assertThat(metaRepo.<Instant>get(getSumsRecomputedAt(UNREADABLE_AVATAR))).isEmpty();
	}

	@Test
	void stampsTheSecondRunsOwnInstantOnEveryAvatarItRefreshesAgain() {
		bankRepo.seedAvatars(List.of(AVATAR, BANK_ONLY_AVATAR));
		storageRepo.seedAvatars(List.of());
		EvergoreDataEvaluator ticking = new EvergoreDataEvaluator(metaRepo, storageRepo, bankRepo, new KnownAvatars(bankRepo, storageRepo), new TickingClock(), logger);
		ticking.evaluateData();

		ticking.evaluateData();

		assertThat(List.of(metaRepo.<Instant>get(getSumsRecomputedAt(AVATAR)), metaRepo.<Instant>get(getSumsRecomputedAt(BANK_ONLY_AVATAR))))
				.containsOnly(Optional.of(FIXED_NOW.plusMillis(1)));
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
	void countsWhatTheGuildPaysAboveItsOwnGoodsValueAsACraftSubsidy() {
		int quantity = 4;
		int quality = 100;
		storageRepo.seedEntries(AVATAR, List.of(storagePlacement(MAGIESPLITTER.ingameName, quantity, quality)));
		storageRepo.seedAvatars(List.of(AVATAR));
		bankRepo.seedAvatars(List.of());

		tested.evaluateData();

		assertThat(metaRepo.<Double>get(getStoragePlacement(AVATAR))).contains(240.0);
		assertThat(metaRepo.<Double>get(getStorageCraftSubsidy(AVATAR))).contains(96.0);
		assertThat(metaRepo.<Double>get(getStorageDonation(AVATAR))).contains(0.0);
	}

	@Test
	void countsADepositThatCreditsNothingAsADonationAtTheGuildsGoodsValue() {
		int quantity = 2;
		int quality = 50;
		storageRepo.seedEntries(AVATAR, List.of(storagePlacement(STERNENSTAUB.ingameName, quantity, quality)));
		storageRepo.seedAvatars(List.of(AVATAR));
		bankRepo.seedAvatars(List.of());

		tested.evaluateData();

		assertThat(metaRepo.<Double>get(getStoragePlacement(AVATAR))).contains(0.0);
		assertThat(metaRepo.<Double>get(getStorageDonation(AVATAR))).contains(72.0);
		assertThat(metaRepo.<Double>get(getStorageCraftSubsidy(AVATAR))).contains(0.0);
	}

	@Test
	void keepsTheTwoFlowsApartOverALedgerThatCarriesBoth() {
		storageRepo.seedEntries(AVATAR, List.of(storagePlacement(STERNENSTAUB.ingameName, 1, 100), storagePlacement(MAGIESPLITTER.ingameName, 4, 100)));
		storageRepo.seedAvatars(List.of(AVATAR));
		bankRepo.seedAvatars(List.of());

		tested.evaluateData();

		assertThat(metaRepo.<Double>get(getStorageDonation(AVATAR))).contains(72.0);
		assertThat(metaRepo.<Double>get(getStorageCraftSubsidy(AVATAR))).contains(96.0);
		assertThat(metaRepo.<Double>get(getStoragePlacement(AVATAR))).contains(240.0);
	}

	@Test
	void countsOnlyDepositsIntoTheTwoFlowsAndNotWithdrawals() {
		storageRepo.seedEntries(AVATAR, List.of(storageWithdrawl(STERNENSTAUB.ingameName, 1, 100)));
		storageRepo.seedAvatars(List.of(AVATAR));
		bankRepo.seedAvatars(List.of());

		tested.evaluateData();

		assertThat(metaRepo.<Double>get(getStorageDonation(AVATAR))).contains(0.0);
		assertThat(metaRepo.<Double>get(getStorageCraftSubsidy(AVATAR))).contains(0.0);
		assertThat(metaRepo.<Double>get(getStorageWithdrawl(AVATAR))).contains(72.0);
	}

	@Test
	void resolvesErdeEibenlanzeByItsRealIngameSpelling() {
		storageRepo.seedEntries(AVATAR, List.of(storagePlacement("Erde-Eibenlanze", 1, 100)));
		storageRepo.seedAvatars(List.of(AVATAR));
		bankRepo.seedAvatars(List.of());

		tested.evaluateData();

		assertThat(metaRepo.<Double>get(getStoragePlacement(AVATAR))).contains(ERDE_EIBENLANZE.getStorageValue());
	}

	@ParameterizedTest(name = "{0} deposited")
	@CsvSource({"Marmor, 72.0", "Granit, 54.0", "Schiefer, 36.0"})
	void creditsADepositOfARawStoneNothingAndBooksItAsADonation(String itemName, double expectedDonation) {
		storageRepo.seedEntries(AVATAR, List.of(storagePlacement(itemName, 1, 100)));
		storageRepo.seedAvatars(List.of(AVATAR));
		bankRepo.seedAvatars(List.of());

		tested.evaluateData();

		assertThat(metaRepo.<Double>get(getStoragePlacement(AVATAR))).contains(0.0);
		assertThat(metaRepo.<Double>get(getStorageDonation(AVATAR))).contains(expectedDonation);
		assertThat(metaRepo.<Double>get(getStorageCraftSubsidy(AVATAR))).contains(0.0);
	}

	@ParameterizedTest(name = "[{0}]")
	@ValueSource(strings = {"marmor", "MARMOR", " Marmor", "Marmor "})
	void reportsASpellingThatIsNotTheOneTheGameUsesAsUnknown(String nearMiss) {
		storageRepo.seedEntries(AVATAR, List.of(storagePlacement(nearMiss, 1, 100)));
		storageRepo.seedAvatars(List.of(AVATAR));
		bankRepo.seedAvatars(List.of());

		EvaluationResult result = tested.evaluateData();

		assertThat(result.unknownItemNames()).containsExactly(nearMiss);
		assertThat(metaRepo.<Double>get(getStorageDonation(AVATAR))).contains(0.0);
	}

	@ParameterizedTest(name = "{0} withdrawn")
	@CsvSource({"Marmor, 72.0", "Granit, 54.0", "Schiefer, 36.0"})
	void valuesAWithdrawalOfARawStoneUnderTheNameTheGameUses(String itemName, double expectedCost) {
		storageRepo.seedEntries(AVATAR, List.of(storageWithdrawl(itemName, 1, 100)));
		storageRepo.seedAvatars(List.of(AVATAR));
		bankRepo.seedAvatars(List.of());

		tested.evaluateData();

		assertThat(metaRepo.<Double>get(getStorageWithdrawl(AVATAR))).contains(expectedCost);
	}

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = {"Streitaxt des Wegelagerers", "Barbarenaxt der Wache", "Bidenaxt des Wegelagerers [2H]"})
	void valuesAMagicallyNamedItemLikeThePlainItemItIsMadeFrom(String affixedName) {
		storageRepo.seedEntries(AVATAR, List.of(storageWithdrawl(affixedName, 1, 100)));
		storageRepo.seedAvatars(List.of(AVATAR));
		bankRepo.seedAvatars(List.of());

		EvaluationResult result = tested.evaluateData();

		assertThat(metaRepo.<Double>get(getStorageWithdrawl(AVATAR))).contains(plainItemNamed(affixedName).getWithdrawlValue());
		assertThat(result.unknownItemNames()).isEmpty();
	}

	private static EvergoreItem plainItemNamed(String affixedName) {
		String plain = affixedName.replaceFirst(" (?:des|der) [A-ZÄÖÜ]\\p{L}+", "");
		return stream(EvergoreItem.values()).filter(item -> item.ingameName.equals(plain)).findAny().orElseThrow();
	}

	@ParameterizedTest(name = "{0}")
	@CsvSource({"Obsidian-Kriegshammer, 82300", "Obsidian-Pike, 82300", "Obsidian-Pike [2H], 82300", "Rubin-Plattenhelm, 15900", "Achat-Armbrust, 5500", "Jade-Feuerstab, 42300"})
	void valuesGemForgedGearAtThePriceTheGameGivesIt(String itemName, int marketValue) {
		storageRepo.seedEntries(AVATAR, List.of(storageWithdrawl(itemName, 1, 100)));
		storageRepo.seedAvatars(List.of(AVATAR));
		bankRepo.seedAvatars(List.of());

		tested.evaluateData();

		assertThat(metaRepo.<Double>get(getStorageWithdrawl(AVATAR))).contains(marketValue * 0.6);
	}

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = {"Übungsstück-Sorandilaxt", "Übungsstück-Eibenstab", "Mystischer Pfeil", "Mystische Essenz"})
	void knowsPracticeGearAndQuestConsumablesAndValuesThemAtZero(String itemName) {
		storageRepo.seedEntries(AVATAR, List.of(storagePlacement(itemName, 7, 100), storageWithdrawl(itemName, 7, 100)));
		storageRepo.seedAvatars(List.of(AVATAR));
		bankRepo.seedAvatars(List.of());

		EvaluationResult result = tested.evaluateData();

		assertThat(result.unknownItemNames()).isEmpty();
		assertThat(metaRepo.<Double>get(getStorageWithdrawl(AVATAR))).contains(0.0);
		assertThat(metaRepo.<Double>get(getStorageDonation(AVATAR))).contains(0.0);
	}

	@Test
	void countsAKnownItemWorthNothingApartFromAnUnknownName() {
		storageRepo.seedEntries(AVATAR, List.of(storagePlacement("Übungsstück-Sorandilaxt", 1, 100), storagePlacement("Unobtainium", 1, 100)));
		storageRepo.seedAvatars(List.of(AVATAR));
		bankRepo.seedAvatars(List.of());

		EvaluationResult result = tested.evaluateData();

		assertThat(result.zeroValuedItemNames()).containsExactly("Übungsstück-Sorandilaxt");
		assertThat(result.unknownItemNames()).containsExactly("Unobtainium");
	}

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = {"Kriegshammer", "Sense", "Speer", "Lanze"})
	void reportsANameTheCatalogDoesNotKnowAsUnknownEvenWhenATwoHandedTwinExists(String bareName) {
		storageRepo.seedEntries(AVATAR, List.of(storageWithdrawl(bareName, 1, 100)));
		storageRepo.seedAvatars(List.of(AVATAR));
		bankRepo.seedAvatars(List.of());

		EvaluationResult result = tested.evaluateData();

		assertThat(result.unknownItemNames()).containsExactly(bareName);
		assertThat(metaRepo.<Double>get(getStorageWithdrawl(AVATAR))).contains(0.0);
	}

	@ParameterizedTest(name = "{0}")
	@CsvSource({"Obsidian-Pike [2H], 82300", "Smaragd-Pike, 22300", "Rubin-Pike [2H], 42300", "Obsidian-Prunkschwert der Entschlossenheit [2H], 82300"})
	void valuesTheSecondSpellingTheLedgerCarriesForOneItem(String itemName, int marketValue) {
		storageRepo.seedEntries(AVATAR, List.of(storageWithdrawl(itemName, 1, 100)));
		storageRepo.seedAvatars(List.of(AVATAR));
		bankRepo.seedAvatars(List.of());

		EvaluationResult result = tested.evaluateData();

		assertThat(metaRepo.<Double>get(getStorageWithdrawl(AVATAR))).contains(marketValue * 0.6);
		assertThat(result.unknownItemNames()).isEmpty();
	}

	@ParameterizedTest(name = "{0}")
	@CsvSource({"Obsidian-Pike des Wegelagerers [2H], 82300", "Rubin-Pike [2H] des Wegelagerers, 42300"})
	void stripsAMagicAffixBeforeTryingTheSecondSpelling(String ledgerName, int marketValue) {
		storageRepo.seedEntries(AVATAR, List.of(storageWithdrawl(ledgerName, 1, 100)));
		storageRepo.seedAvatars(List.of(AVATAR));
		bankRepo.seedAvatars(List.of());

		EvaluationResult result = tested.evaluateData();

		assertThat(metaRepo.<Double>get(getStorageWithdrawl(AVATAR))).contains(marketValue * 0.6);
		assertThat(result.unknownItemNames()).isEmpty();
	}

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = {"Streitaxt des Dunklen Waldes", "Streitaxt der Wache des Nordens", "Streitaxt des wegelagerers"})
	void reportsAnAffixShapeTheLookupDoesNotHandleAsUnknownRatherThanValuingItWrong(String ledgerName) {
		storageRepo.seedEntries(AVATAR, List.of(storageWithdrawl(ledgerName, 1, 100)));
		storageRepo.seedAvatars(List.of(AVATAR));
		bankRepo.seedAvatars(List.of());

		EvaluationResult result = tested.evaluateData();

		assertThat(result.unknownItemNames()).containsExactly(ledgerName);
		assertThat(metaRepo.<Double>get(getStorageWithdrawl(AVATAR))).contains(0.0);
	}

	@Test
	void reportsNoRawStoneNameAsUnknown() {
		storageRepo.seedEntries(AVATAR, List.of(storagePlacement("Marmor", 1, 100), storagePlacement("Granit", 1, 100), storagePlacement("Schiefer", 1, 100)));
		storageRepo.seedAvatars(List.of(AVATAR));
		bankRepo.seedAvatars(List.of());

		EvaluationResult result = tested.evaluateData();

		assertThat(result.unknownItemNames()).isEmpty();
	}

	@Test
	void unknownItemFallsBackToZeroValueAndLogsAWarning() {
		String unknownItemName = "Unobtainium";
		storageRepo.seedEntries(AVATAR, List.of(storagePlacement(unknownItemName, 1, 100)));
		storageRepo.seedAvatars(List.of(AVATAR));
		bankRepo.seedAvatars(List.of());

		tested.evaluateData();

		assertThat(metaRepo.<Double>get(getStoragePlacement(AVATAR))).contains(0.0);
		assertThat(logger.warnMessages()).contains("Unable to find item: " + unknownItemName);
	}

	@Test
	void evaluationResultCountsEachUnknownItemOccurrence() {
		String unknownItemName = "Unobtainium";
		storageRepo.seedEntries(AVATAR, List.of(storagePlacement(unknownItemName, 1, 100), storagePlacement(unknownItemName, 2, 100)));
		storageRepo.seedAvatars(List.of(AVATAR));
		bankRepo.seedAvatars(List.of());

		EvaluationResult result = tested.evaluateData();

		assertThat(result.unknownItemNames()).containsExactly(unknownItemName, unknownItemName);
	}

	@Test
	void evaluationResultHasNoUnknownItemsWhenEveryItemResolves() {
		storageRepo.seedEntries(AVATAR, List.of(storagePlacement(LEINENTUCH.ingameName, 1, 100)));
		storageRepo.seedAvatars(List.of(AVATAR));
		bankRepo.seedAvatars(List.of());

		EvaluationResult result = tested.evaluateData();

		assertThat(result.unknownItemNames()).isEmpty();
	}

	@Test
	void overwritesAPreviouslyStoredValueWithTheFreshRecompute() {
		metaRepo.put(getStoragePlacement(AVATAR), 999.0);
		storageRepo.seedEntries(AVATAR, List.of(storagePlacement(LEINENTUCH.ingameName, 1, 100)));
		storageRepo.seedAvatars(List.of(AVATAR));
		bankRepo.seedAvatars(List.of());

		tested.evaluateData();

		double expected = LEINENTUCH.getStorageValue() * 1 * 1.0;
		assertThat(metaRepo.<Double>get(getStoragePlacement(AVATAR))).contains(expected);
	}

	@Test
	void evaluatingTwiceYieldsIdenticalSums() {
		bankRepo.seedEntries(AVATAR, List.of(bankPlacement(100)));
		bankRepo.seedAvatars(List.of(AVATAR));
		storageRepo.seedAvatars(List.of());

		tested.evaluateData();
		tested.evaluateData();

		assertThat(metaRepo.<Long>get(getBankPlacement(AVATAR))).contains(100L);
	}

	@Test
	void selfHealsAfterAMidRunFailureOnANextSuccessfulRun() {
		FlakyStorageRepositoryStub flakyStorage = new FlakyStorageRepositoryStub();
		flakyStorage.seedEntries(AVATAR, List.of(storagePlacement(LEINENTUCH.ingameName, 1, 100)));
		flakyStorage.seedAvatars(List.of(AVATAR));
		bankRepo.seedAvatars(List.of());
		EvergoreDataEvaluator flakyEvaluator = new EvergoreDataEvaluator(metaRepo, flakyStorage, bankRepo, new KnownAvatars(bankRepo, flakyStorage),
				Clock.fixed(FIXED_NOW, ZoneOffset.UTC), logger);
		flakyStorage.failOnNextCall = true;

		assertThat(flakyEvaluator.evaluateData().failedAvatarNames()).containsExactly(AVATAR);
		flakyEvaluator.evaluateData();

		double expected = LEINENTUCH.getStorageValue() * 1 * 1.0;
		assertThat(metaRepo.<Double>get(getStoragePlacement(AVATAR))).contains(expected);
	}

	@Test
	void stampsTheCollectionTimestampEvenWhileOneAvatarFailedToRecompute() {
		FlakyStorageRepositoryStub flakyStorage = new FlakyStorageRepositoryStub();
		flakyStorage.seedAvatars(List.of(AVATAR));
		bankRepo.seedAvatars(List.of());
		EvergoreDataEvaluator flakyEvaluator = new EvergoreDataEvaluator(metaRepo, flakyStorage, bankRepo, new KnownAvatars(bankRepo, flakyStorage),
				Clock.fixed(FIXED_NOW, ZoneOffset.UTC), logger);
		flakyStorage.failOnNextCall = true;

		assertThat(flakyEvaluator.evaluateData().failedAvatarNames()).containsExactly(AVATAR);

		assertThat(metaRepo.<LocalDateTime>get(getLastUpdatedKey())).contains(LocalDateTime.ofInstant(FIXED_NOW, ZoneOffset.UTC));
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
	void writesLastUpdatedInBerlinWallClockNotUtcAfterASuccessfulRun() {
		Instant nearMidnightUtc = Instant.parse("2026-06-21T23:30:00Z");
		tested = new EvergoreDataEvaluator(metaRepo, storageRepo, bankRepo, new KnownAvatars(bankRepo, storageRepo), Clock.fixed(nearMidnightUtc, APP_ZONE), logger);
		bankRepo.seedAvatars(List.of());
		storageRepo.seedAvatars(List.of());

		tested.evaluateData();

		assertThat(metaRepo.<LocalDateTime>get(getLastUpdatedKey())).contains(LocalDateTime.of(2026, 6, 22, 1, 30));
	}

	@Test
	void applicationClockUsesTheBerlinZone() {
		assertThat(new ApplicationFactory().clock().getZone()).isEqualTo(APP_ZONE);
	}

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

	private static class FlakyStorageRepositoryStub extends StorageRepositoryStub {
		private boolean failOnNextCall;

		@Override
		public List<StorageEntry> getAllFor(String avatar) {
			if (failOnNextCall) {
				failOnNextCall = false;
				throw new RuntimeException("storage lookup failed");
			}
			return super.getAllFor(avatar);
		}
	}

	private static final class TickingClock extends Clock {
		private Instant now = FIXED_NOW;

		@Override
		public Instant instant() {
			Instant current = now;
			now = now.plusMillis(1);
			return current;
		}

		@Override
		public ZoneId getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(ZoneId zone) {
			return this;
		}
	}
}
