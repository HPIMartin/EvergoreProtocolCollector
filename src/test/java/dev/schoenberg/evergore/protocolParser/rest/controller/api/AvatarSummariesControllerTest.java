package dev.schoenberg.evergore.protocolParser.rest.controller.api;

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

import static dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey.getBankPlacement;
import static dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey.getBankWithdrawl;
import static dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey.getStoragePlacement;
import static dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey.getStorageWithdrawl;
import static org.assertj.core.api.Assertions.assertThat;

class AvatarSummariesControllerTest {
	private static final int WHOLE_PAGE = 100;

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

		assertThat(page.items()).containsExactly(new AvatarSummary("Brynja", 0, 0, 0, 0, 0, null, null));
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

	private static List<String> avatarsOf(AvatarSummaryPage page) {
		return page.items().stream().map(AvatarSummary::avatar).toList();
	}
}
