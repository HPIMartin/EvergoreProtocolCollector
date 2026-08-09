package dev.schoenberg.evergore.protocolParser.rest.controller.api;

import java.util.List;

import org.junit.jupiter.api.Test;

import dev.schoenberg.evergore.protocolParser.LoggerSpy;
import dev.schoenberg.evergore.protocolParser.businessLogic.banking.BankRepositoryStub;
import dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.FakeMetaInformationRepository;
import dev.schoenberg.evergore.protocolParser.businessLogic.storage.StorageRepositoryStub;
import dev.schoenberg.evergore.protocolParser.rest.controller.api.wire.AvatarSummary;
import dev.schoenberg.evergore.protocolParser.rest.controller.api.wire.AvatarSummaryPage;

import static org.assertj.core.api.Assertions.assertThat;

class AvatarSummariesControllerTest {
	private static final int WHOLE_PAGE = 100;

	private final FakeMetaInformationRepository metaRepo = new FakeMetaInformationRepository();
	private final BankRepositoryStub bankRepo = new BankRepositoryStub();
	private final StorageRepositoryStub storageRepo = new StorageRepositoryStub();
	private final AvatarSummariesController tested = new AvatarSummariesController(metaRepo, bankRepo, storageRepo, new LoggerSpy());

	@Test
	void listsAnAvatarThatOnlyEverMovedItems() {
		bankRepo.seedAvatars(List.of("Aurora"));
		storageRepo.seedAvatars(List.of("Brynja"));

		AvatarSummaryPage page = tested.summaries(0, WHOLE_PAGE);

		assertThat(avatarsOf(page)).containsExactly("Aurora", "Brynja");
	}

	@Test
	void namesAnAvatarPresentInBothLedgersOnlyOnce() {
		bankRepo.seedAvatars(List.of("Aurora", "Boreas"));
		storageRepo.seedAvatars(List.of("Aurora"));

		AvatarSummaryPage page = tested.summaries(0, WHOLE_PAGE);

		assertThat(avatarsOf(page)).containsExactly("Aurora", "Boreas");
	}

	@Test
	void sortsTheUnionAsAWholeRatherThanAppendingTheStorageAvatars() {
		bankRepo.seedAvatars(List.of("Aurora", "Calix"));
		storageRepo.seedAvatars(List.of("Brynja"));

		AvatarSummaryPage page = tested.summaries(0, WHOLE_PAGE);

		assertThat(avatarsOf(page)).containsExactly("Aurora", "Brynja", "Calix");
	}

	@Test
	void countsTheUnionRatherThanOneLedgerWhenAPageShowsPartOfIt() {
		bankRepo.seedAvatars(List.of("Aurora", "Calix"));
		storageRepo.seedAvatars(List.of("Brynja"));

		AvatarSummaryPage page = tested.summaries(0, 1);

		assertThat(page.totalCount()).isEqualTo(3);
	}

	@Test
	void countsAnAvatarPresentInBothLedgersOnlyOnce() {
		bankRepo.seedAvatars(List.of("Aurora", "Boreas"));
		storageRepo.seedAvatars(List.of("Aurora"));

		AvatarSummaryPage page = tested.summaries(0, WHOLE_PAGE);

		assertThat(page.totalCount()).isEqualTo(2);
	}

	@Test
	void carriesZeroGoldForAnAvatarThatOnlyEverMovedItems() {
		bankRepo.seedAvatars(List.of());
		storageRepo.seedAvatars(List.of("Brynja"));

		AvatarSummaryPage page = tested.summaries(0, WHOLE_PAGE);

		assertThat(page.items()).containsExactly(new AvatarSummary("Brynja", 0, 0));
	}

	private static List<String> avatarsOf(AvatarSummaryPage page) {
		return page.items().stream().map(AvatarSummary::avatar).toList();
	}
}
