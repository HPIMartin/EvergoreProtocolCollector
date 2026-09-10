package dev.schoenberg.evergore.protocolParser.rest.controller.api;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import dev.schoenberg.evergore.protocolParser.LoggerSpy;
import dev.schoenberg.evergore.protocolParser.businessLogic.KnownAvatars;
import dev.schoenberg.evergore.protocolParser.businessLogic.banking.BankRepositoryStub;
import dev.schoenberg.evergore.protocolParser.businessLogic.contribution.AvatarContributions;
import dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.FakeMetaInformationRepository;
import dev.schoenberg.evergore.protocolParser.businessLogic.storage.StorageRepositoryStub;
import dev.schoenberg.evergore.protocolParser.rest.controller.api.wire.AvatarSummary;
import dev.schoenberg.evergore.protocolParser.rest.controller.api.wire.AvatarSummaryPage;
import dev.schoenberg.evergore.protocolParser.rest.controller.api.wire.GuildTotals;

import static dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey.getBankPlacement;
import static dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey.getBankWithdrawl;
import static dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey.getStorageCraftSubsidy;
import static dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey.getStorageDonation;
import static dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey.getStoragePlacement;
import static dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey.getStorageWithdrawl;
import static dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey.getSumsRecomputedAt;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class AvatarSummariesControllerTest {
	private static final int WHOLE_PAGE = 100;
	private static final Instant THE_RUN_BEFORE = Instant.parse("2026-09-08T01:12:00Z");
	private static final Instant LAST_COLLECTION = Instant.parse("2026-09-09T01:12:00Z");

	private final FakeMetaInformationRepository metaRepo = new FakeMetaInformationRepository();
	private final BankRepositoryStub bankRepo = new BankRepositoryStub();
	private final StorageRepositoryStub storageRepo = new StorageRepositoryStub();
	private final AvatarContributions contributions = new AvatarContributions(new KnownAvatars(bankRepo, storageRepo), metaRepo, bankRepo, storageRepo);
	private final AvatarSummariesController tested = new AvatarSummariesController(contributions, new LoggerSpy());

	@Test
	void listsAnAvatarThatOnlyEverMovedItems() {
		bankRepo.seedAvatars(List.of("Aurora"));
		storageRepo.seedAvatars(List.of("Brynja"));

		AvatarSummaryPage page = tested.summaries(0, WHOLE_PAGE);

		assertThat(avatarsOf(page)).containsExactly("Aurora", "Brynja");
	}

	@Test
	void countsEveryKnownAvatarWhileAPageShowsOnlyPartOfThem() {
		bankRepo.seedAvatars(List.of("Aurora", "Calix"));
		storageRepo.seedAvatars(List.of("Brynja"));

		AvatarSummaryPage page = tested.summaries(0, 1);

		assertThat(page.totalCount()).isEqualTo(3);
	}

	@Test
	void carriesZerosForAnAvatarWithoutAnyStoredSum() {
		bankRepo.seedAvatars(List.of());
		storageRepo.seedAvatars(List.of("Brynja"));

		AvatarSummaryPage page = tested.summaries(0, WHOLE_PAGE);

		assertThat(page.items()).containsExactly(new AvatarSummary("Brynja", 0, 0, 0, 0, 0, null, null, null, null, null));
	}

	@Test
	void roundsAFractionalStorageSumBeforeServingItRatherThanTruncatingTheNet() {
		bankRepo.seedAvatars(List.of("Aurora"));
		metaRepo.put(getStoragePlacement("Aurora"), 0.4);
		metaRepo.put(getStorageWithdrawl("Aurora"), 1.0);

		AvatarSummary summary = tested.summaries(0, WHOLE_PAGE).items().get(0);

		assertThat(summary.net()).isEqualTo(-1L);
	}

	@Test
	void servesBothFlowsOfTheGuildShareBesideTheLedgerSums() {
		bankRepo.seedAvatars(List.of("Aurora"));
		metaRepo.put(getStoragePlacement("Aurora"), 308.4);
		metaRepo.put(getStorageWithdrawl("Aurora"), 300.0);
		metaRepo.put(getStorageDonation("Aurora"), 140.0);
		metaRepo.put(getStorageCraftSubsidy("Aurora"), 20.0);

		AvatarSummary summary = tested.summaries(0, WHOLE_PAGE).items().get(0);

		assertThat(summary.donation()).isEqualTo(140L);
		assertThat(summary.craftSubsidy()).isEqualTo(20L);
	}

	@Test
	void servesNeitherFlowWhileNoRecomputeHasProducedThemYet() {
		bankRepo.seedAvatars(List.of("Aurora"));
		metaRepo.put(getStoragePlacement("Aurora"), 308.4);
		metaRepo.put(getStorageWithdrawl("Aurora"), 300.0);

		AvatarSummary summary = tested.summaries(0, WHOLE_PAGE).items().get(0);

		assertThat(summary.donation()).isNull();
		assertThat(summary.craftSubsidy()).isNull();
	}

	@Test
	void servesNeitherFlowWhileOnlyOneOfTheTwoWasStored() {
		bankRepo.seedAvatars(List.of("Aurora"));
		metaRepo.put(getStorageDonation("Aurora"), 140.0);

		AvatarSummary summary = tested.summaries(0, WHOLE_PAGE).items().get(0);

		assertThat(summary.donation()).isNull();
	}

	@Test
	void servesNoGuildFlowsWhileOneAvatarIsMissingHisOwn() {
		bankRepo.seedAvatars(List.of("Aurora", "Boreas"));
		metaRepo.put(getStorageDonation("Aurora"), 140.0);
		metaRepo.put(getStorageCraftSubsidy("Aurora"), 20.0);

		GuildTotals totals = tested.summaries(0, WHOLE_PAGE).totals();

		assertThat(totals.donation()).isNull();
	}

	@Test
	void keepsTheGuildFlowsAndEveryRowsFlowsFromDisagreeingAboutWhatIsKnown() {
		bankRepo.seedAvatars(List.of("Aurora", "Boreas"));
		metaRepo.put(getStorageDonation("Aurora"), 140.0);
		metaRepo.put(getStorageCraftSubsidy("Aurora"), 20.0);

		AvatarSummaryPage page = tested.summaries(0, WHOLE_PAGE);

		assertThat(page.totals().donation()).isNull();
		assertThat(page.items()).extracting(AvatarSummary::donation).containsExactly(140L, null);
	}

	@Test
	void servesTheGuildFlowsAsTheSumOfEveryAvatarsOwn() {
		bankRepo.seedAvatars(List.of("Aurora", "Boreas"));
		metaRepo.put(getStorageDonation("Aurora"), 140.0);
		metaRepo.put(getStorageCraftSubsidy("Aurora"), 20.0);
		metaRepo.put(getStorageDonation("Boreas"), 60.0);
		metaRepo.put(getStorageCraftSubsidy("Boreas"), 5.0);

		GuildTotals totals = tested.summaries(0, WHOLE_PAGE).totals();

		assertThat(totals.donation()).isEqualTo(200L);
		assertThat(totals.craftSubsidy()).isEqualTo(25L);
	}

	@Test
	void servesBothStorageSumsAndTheNetAsWholeGoldBesideTheBankSums() {
		bankRepo.seedAvatars(List.of("Aurora"));
		metaRepo.put(getBankPlacement("Aurora"), 1500L);
		metaRepo.put(getBankWithdrawl("Aurora"), 200L);
		metaRepo.put(getStoragePlacement("Aurora"), 185.04);
		metaRepo.put(getStorageWithdrawl("Aurora"), 300.0);

		AvatarSummary summary = tested.summaries(0, WHOLE_PAGE).items().get(0);

		assertThat(summary.bankDeposited()).isEqualTo(1500);
		assertThat(summary.bankWithdrawn()).isEqualTo(200);
		assertThat(summary.storageDeposited()).isEqualTo(185);
		assertThat(summary.storageWithdrawn()).isEqualTo(300);
		assertThat(summary.net()).isEqualTo(1185);
	}

	@Test
	void totalsEveryKnownAvatarRatherThanOnlyTheAvatarsOfTheServedPage() {
		bankRepo.seedAvatars(List.of("Aurora", "Calix"));
		metaRepo.put(getBankPlacement("Aurora"), 1500L);
		metaRepo.put(getBankPlacement("Calix"), 500L);

		AvatarSummaryPage page = tested.summaries(0, 1);

		assertThat(page.items()).hasSize(1);
		assertThat(page.totals().bankDeposited()).isEqualTo(2000);
	}

	@Test
	void totalsTheWholeGoldNetOfEveryKnownAvatarAcrossBothLedgers() {
		bankRepo.seedAvatars(List.of("Aurora"));
		storageRepo.seedAvatars(List.of("Brynja"));
		metaRepo.put(getBankPlacement("Aurora"), 1500L);
		metaRepo.put(getStorageWithdrawl("Aurora"), 300.0);
		metaRepo.put(getStoragePlacement("Brynja"), 370.08);

		AvatarSummaryPage page = tested.summaries(0, WHOLE_PAGE);

		assertThat(page.totals().net()).isEqualTo(1570);
	}

	@Test
	void totalsTheRoundedContributionsRatherThanRoundingTheGuildsTrueSum() {
		bankRepo.seedAvatars(List.of("Aurora", "Boreas", "Calla"));
		metaRepo.put(getStoragePlacement("Aurora"), 100.4);
		metaRepo.put(getStoragePlacement("Boreas"), 100.4);
		metaRepo.put(getStoragePlacement("Calla"), 100.4);

		AvatarSummaryPage page = tested.summaries(0, WHOLE_PAGE);

		assertThat(page.totals().storageDeposited()).isEqualTo(300);
	}

	@Test
	void servesNoStaleInstantForARowThatTheLastCollectionRefreshed() {
		bankRepo.seedAvatars(List.of("Aurora", "Calix"));
		metaRepo.put(getSumsRecomputedAt("Aurora"), LAST_COLLECTION);
		metaRepo.put(getSumsRecomputedAt("Calix"), LAST_COLLECTION);

		AvatarSummaryPage page = tested.summaries(0, WHOLE_PAGE);

		assertThat(page.items()).extracting(AvatarSummary::staleSumsFrom).containsOnlyNulls();
		assertThat(page.totals().containsStaleSums()).isFalse();
	}

	@Test
	void servesTheInstantARowsSumsComeFromWhenTheLastCollectionMissedIt() {
		bankRepo.seedAvatars(List.of("Aurora", "Calix"));
		metaRepo.put(getSumsRecomputedAt("Aurora"), THE_RUN_BEFORE);
		metaRepo.put(getSumsRecomputedAt("Calix"), LAST_COLLECTION);

		AvatarSummaryPage page = tested.summaries(0, WHOLE_PAGE);

		assertThat(page.items()).extracting(AvatarSummary::avatar, AvatarSummary::staleSumsFrom).containsExactly(tuple("Aurora", THE_RUN_BEFORE), tuple("Calix", null));
	}

	@Test
	void statesThatTheGuildTotalContainsStaleSumsEvenWhenThePageDoesNotShowThatRow() {
		bankRepo.seedAvatars(List.of("Aurora", "Calix"));
		metaRepo.put(getSumsRecomputedAt("Aurora"), LAST_COLLECTION);
		metaRepo.put(getSumsRecomputedAt("Calix"), THE_RUN_BEFORE);

		AvatarSummaryPage page = tested.summaries(0, 1);

		assertThat(page.items()).extracting(AvatarSummary::avatar).containsExactly("Aurora");
		assertThat(page.totals().containsStaleSums()).isTrue();
	}

	private static List<String> avatarsOf(AvatarSummaryPage page) {
		return page.items().stream().map(AvatarSummary::avatar).toList();
	}
}
