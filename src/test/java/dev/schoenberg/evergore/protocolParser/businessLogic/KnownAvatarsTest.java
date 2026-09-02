package dev.schoenberg.evergore.protocolParser.businessLogic;

import java.util.List;

import org.junit.jupiter.api.Test;

import dev.schoenberg.evergore.protocolParser.businessLogic.banking.BankRepositoryStub;
import dev.schoenberg.evergore.protocolParser.businessLogic.storage.StorageRepositoryStub;

import static org.assertj.core.api.Assertions.assertThat;

class KnownAvatarsTest {
	private final BankRepositoryStub bankRepo = new BankRepositoryStub();
	private final StorageRepositoryStub storageRepo = new StorageRepositoryStub();
	private final KnownAvatars tested = new KnownAvatars(bankRepo, storageRepo);

	@Test
	void namesAnAvatarThatOnlyEverMovedItems() {
		bankRepo.seedAvatars(List.of("Aurora"));
		storageRepo.seedAvatars(List.of("Brynja"));

		List<String> known = tested.sortedByName();

		assertThat(known).containsExactly("Aurora", "Brynja");
	}

	@Test
	void namesAnAvatarPresentInBothLedgersOnlyOnce() {
		bankRepo.seedAvatars(List.of("Aurora", "Boreas"));
		storageRepo.seedAvatars(List.of("Aurora"));

		List<String> known = tested.sortedByName();

		assertThat(known).containsExactly("Aurora", "Boreas");
	}

	@Test
	void sortsBothLedgersTogetherRatherThanAppendingTheStorageAvatars() {
		bankRepo.seedAvatars(List.of("Aurora", "Calix"));
		storageRepo.seedAvatars(List.of("Brynja"));

		List<String> known = tested.sortedByName();

		assertThat(known).containsExactly("Aurora", "Brynja", "Calix");
	}

	@Test
	void sortsAnUmlautNameWhereGermanCollationPutsItRatherThanBehindZ() {
		bankRepo.seedAvatars(List.of("Zorn", "Ärger", "Anna"));
		storageRepo.seedAvatars(List.of());

		List<String> known = tested.sortedByName();

		assertThat(known).containsExactly("Anna", "Ärger", "Zorn");
	}

	@Test
	void namesNobodyWhileNeitherLedgerHasRows() {
		bankRepo.seedAvatars(List.of());
		storageRepo.seedAvatars(List.of());

		List<String> known = tested.sortedByName();

		assertThat(known).isEmpty();
	}
}
