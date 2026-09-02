package dev.schoenberg.evergore.protocolParser.businessLogic.contribution;

import java.util.List;

import org.junit.jupiter.api.Test;

import dev.schoenberg.evergore.protocolParser.businessLogic.KnownAvatars;
import dev.schoenberg.evergore.protocolParser.businessLogic.banking.BankRepositoryStub;
import dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.FakeMetaInformationRepository;
import dev.schoenberg.evergore.protocolParser.businessLogic.storage.StorageRepositoryStub;

import static dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey.getBankPlacement;
import static dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey.getBankWithdrawl;
import static dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey.getStoragePlacement;
import static dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey.getStorageWithdrawl;
import static org.assertj.core.api.Assertions.assertThat;

class AvatarContributionsTest {
	private final FakeMetaInformationRepository metaRepo = new FakeMetaInformationRepository();
	private final BankRepositoryStub bankRepo = new BankRepositoryStub();
	private final StorageRepositoryStub storageRepo = new StorageRepositoryStub();
	private final AvatarContributions tested = new AvatarContributions(new KnownAvatars(bankRepo, storageRepo), metaRepo);

	@Test
	void readsAllFourStoredSumsOfAKnownAvatar() {
		bankRepo.seedAvatars(List.of("Aurora"));
		metaRepo.put(getBankPlacement("Aurora"), 1500L);
		metaRepo.put(getBankWithdrawl("Aurora"), 200L);
		metaRepo.put(getStoragePlacement("Aurora"), 185.04);
		metaRepo.put(getStorageWithdrawl("Aurora"), 300.0);

		List<AvatarContribution> all = tested.ofEveryKnownAvatar();

		assertThat(all).containsExactly(new AvatarContribution("Aurora", new Contribution(1500, 200, 185.04, 300.0)));
	}

	@Test
	void countsAnAvatarWithoutAnyStoredSumAsZeroRatherThanLeavingHimOut() {
		storageRepo.seedAvatars(List.of("Brynja"));

		List<AvatarContribution> all = tested.ofEveryKnownAvatar();

		assertThat(all).containsExactly(new AvatarContribution("Brynja", Contribution.NOTHING));
	}

	@Test
	void keepsTheGermanCollationOrderOfTheKnownAvatars() {
		bankRepo.seedAvatars(List.of("Zorn", "Ärger", "Anna"));

		List<String> named = tested.ofEveryKnownAvatar().stream().map(AvatarContribution::avatar).toList();

		assertThat(named).containsExactly("Anna", "Ärger", "Zorn");
	}
}
