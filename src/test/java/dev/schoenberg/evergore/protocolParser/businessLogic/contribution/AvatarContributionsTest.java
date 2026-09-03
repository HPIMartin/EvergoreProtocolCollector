package dev.schoenberg.evergore.protocolParser.businessLogic.contribution;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import dev.schoenberg.evergore.protocolParser.businessLogic.KnownAvatars;
import dev.schoenberg.evergore.protocolParser.businessLogic.banking.BankEntry;
import dev.schoenberg.evergore.protocolParser.businessLogic.banking.BankRepositoryStub;
import dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.FakeMetaInformationRepository;
import dev.schoenberg.evergore.protocolParser.businessLogic.storage.StorageEntry;
import dev.schoenberg.evergore.protocolParser.businessLogic.storage.StorageRepositoryStub;

import static dev.schoenberg.evergore.protocolParser.businessLogic.base.TransferType.EINLAGERUNG;
import static dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey.getBankPlacement;
import static dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey.getBankWithdrawl;
import static dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey.getStoragePlacement;
import static dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey.getStorageWithdrawl;
import static org.assertj.core.api.Assertions.assertThat;

class AvatarContributionsTest {
	private final FakeMetaInformationRepository metaRepo = new FakeMetaInformationRepository();
	private final BankRepositoryStub bankRepo = new BankRepositoryStub();
	private final StorageRepositoryStub storageRepo = new StorageRepositoryStub();
	private final AvatarContributions tested = new AvatarContributions(new KnownAvatars(bankRepo, storageRepo), metaRepo, bankRepo, storageRepo);

	private static final Instant EARLIER = Instant.parse("2024-01-10T09:00:00Z");
	private static final Instant LATER = Instant.parse("2024-01-12T11:00:00Z");

	@Test
	void readsTheWholeGuildFromASingleSnapshotOfTheStore() {
		bankRepo.seedAvatars(List.of("Aurora", "Brynja", "Calix"));

		tested.ofEveryKnownAvatar();

		assertThat(metaRepo.takenSnapshots()).isEqualTo(1);
	}

	@Test
	void readsAllFourStoredSumsOfAKnownAvatar() {
		bankRepo.seedAvatars(List.of("Aurora"));
		metaRepo.put(getBankPlacement("Aurora"), 1500L);
		metaRepo.put(getBankWithdrawl("Aurora"), 200L);
		metaRepo.put(getStoragePlacement("Aurora"), 185.04);
		metaRepo.put(getStorageWithdrawl("Aurora"), 300.0);

		List<AvatarContribution> all = tested.ofEveryKnownAvatar().avatars();

		assertThat(all).containsExactly(new AvatarContribution("Aurora", new Contribution(1500, 200, 185.04, 300.0), null, null));
	}

	@Test
	void countsAnAvatarWithoutAnyStoredSumAsZeroRatherThanLeavingHimOut() {
		storageRepo.seedAvatars(List.of("Brynja"));

		List<AvatarContribution> all = tested.ofEveryKnownAvatar().avatars();

		assertThat(all).containsExactly(new AvatarContribution("Brynja", Contribution.NOTHING, null, null));
	}

	@Test
	void namesTheNewestEntryOfEachLedgerAsThatAvatarsLastActivity() {
		bankRepo.seedAvatars(List.of("Aurora"));
		bankRepo.seedEntries("Aurora", List.of(new BankEntry(EARLIER, "Aurora", 100, EINLAGERUNG), new BankEntry(LATER, "Aurora", 200, EINLAGERUNG)));
		storageRepo.seedEntries("Aurora", List.of(new StorageEntry(EARLIER, "Aurora", 1, "Kupfererz", 100, EINLAGERUNG)));

		AvatarContribution aurora = tested.ofEveryKnownAvatar().avatars().getFirst();

		assertThat(aurora.lastBankActivity()).isEqualTo(LATER);
		assertThat(aurora.lastStorageActivity()).isEqualTo(EARLIER);
	}

	@Test
	void leavesTheLastActivityOfALedgerTheAvatarNeverUsedUnanswered() {
		storageRepo.seedAvatars(List.of("Brynja"));
		storageRepo.seedEntries("Brynja", List.of(new StorageEntry(EARLIER, "Brynja", 4, "Magische Ätherbinde", 100, EINLAGERUNG)));

		AvatarContribution brynja = tested.ofEveryKnownAvatar().avatars().getFirst();

		assertThat(brynja.lastBankActivity()).isNull();
		assertThat(brynja.lastStorageActivity()).isEqualTo(EARLIER);
	}

	@Test
	void keepsTheGermanCollationOrderOfTheKnownAvatars() {
		bankRepo.seedAvatars(List.of("Zorn", "Ärger", "Anna"));

		List<String> named = tested.ofEveryKnownAvatar().avatars().stream().map(AvatarContribution::avatar).toList();

		assertThat(named).containsExactly("Anna", "Ärger", "Zorn");
	}
}
