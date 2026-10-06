package dev.schoenberg.evergore.protocolParser.businessLogic.contribution;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import dev.schoenberg.evergore.protocolParser.businessLogic.KnownAvatars;
import dev.schoenberg.evergore.protocolParser.businessLogic.banking.BankEntry;
import dev.schoenberg.evergore.protocolParser.businessLogic.banking.BankRepositoryStub;
import dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.FakeMetaInformationRepository;
import dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformation;
import dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey;
import dev.schoenberg.evergore.protocolParser.businessLogic.storage.StorageEntry;
import dev.schoenberg.evergore.protocolParser.businessLogic.storage.StorageRepositoryStub;

import static dev.schoenberg.evergore.protocolParser.businessLogic.base.TransferType.EINLAGERUNG;
import static dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey.getBankPlacement;
import static dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey.getBankWithdrawl;
import static dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey.getStoragePlacement;
import static dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey.getStorageWithdrawl;
import static dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey.getSumsRecomputedAt;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

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
	void reportsNoStaleSumsWhileEveryAvatarWasRecomputedInTheSameRun() {
		bankRepo.seedAvatars(List.of("Aurora", "Brynja"));
		recomputed("Aurora", LATER);
		recomputed("Brynja", LATER);

		List<AvatarContribution> all = tested.ofEveryKnownAvatar().avatars();

		assertThat(all).extracting(AvatarContribution::staleSumsFrom).containsOnlyNulls();
	}

	@Test
	void reportsTheStoredInstantOfAnAvatarWhoseSumsAreOlderThanTheLastCollection() {
		bankRepo.seedAvatars(List.of("Aurora", "Brynja"));
		recomputed("Aurora", EARLIER);
		recomputed("Brynja", LATER);

		List<AvatarContribution> all = tested.ofEveryKnownAvatar().avatars();

		assertThat(all).extracting(AvatarContribution::avatar, AvatarContribution::staleSumsFrom).containsExactly(tuple("Aurora", EARLIER), tuple("Brynja", null));
	}

	@Test
	void reportsNoStaleSumsWhileNoAvatarCarriesARecomputeInstant() {
		bankRepo.seedAvatars(List.of("Aurora", "Brynja"));

		List<AvatarContribution> all = tested.ofEveryKnownAvatar().avatars();

		assertThat(all).extracting(AvatarContribution::staleSumsFrom).containsOnlyNulls();
	}

	@Test
	void reportsNoStaleSumsForAnAvatarThatCarriesNoRecomputeInstantAtAll() {
		bankRepo.seedAvatars(List.of("Aurora", "Brynja"));
		metaRepo.put(getBankPlacement("Aurora"), 0L);
		metaRepo.put(getBankWithdrawl("Aurora"), 0L);
		metaRepo.put(getStoragePlacement("Aurora"), 0.0);
		metaRepo.put(getStorageWithdrawl("Aurora"), 0.0);
		recomputed("Brynja", LATER);

		List<AvatarContribution> all = tested.ofEveryKnownAvatar().avatars();

		assertThat(all).extracting(AvatarContribution::avatar, AvatarContribution::staleSumsFrom).containsExactly(tuple("Aurora", null), tuple("Brynja", null));
	}

	@Test
	void statesThatTheGuildContainsStaleSumsWhenOneAvatarLagsBehindTheLastCollection() {
		bankRepo.seedAvatars(List.of("Aurora", "Brynja"));
		recomputed("Aurora", EARLIER);
		recomputed("Brynja", LATER);

		assertThat(tested.ofEveryKnownAvatar().containsStaleSums()).isTrue();
	}

	@Test
	void statesThatTheGuildContainsNoStaleSumsWhileEveryAvatarIsCurrent() {
		bankRepo.seedAvatars(List.of("Aurora", "Brynja"));
		recomputed("Aurora", LATER);
		recomputed("Brynja", LATER);

		assertThat(tested.ofEveryKnownAvatar().containsStaleSums()).isFalse();
	}

	@Test
	void readsAllFourStoredSumsOfAKnownAvatar() {
		bankRepo.seedAvatars(List.of("Aurora"));
		metaRepo.put(getBankPlacement("Aurora"), 1500L);
		metaRepo.put(getBankWithdrawl("Aurora"), 200L);
		metaRepo.put(getStoragePlacement("Aurora"), 185.04);
		metaRepo.put(getStorageWithdrawl("Aurora"), 300.0);

		List<AvatarContribution> all = tested.ofEveryKnownAvatar().avatars();

		assertThat(all).containsExactly(new AvatarContribution("Aurora", Optional.of(new Contribution(1500, 200, 185.04, 300.0, Optional.empty())), null, null, null));
	}

	@Test
	void answersNoContributionForAnAvatarNoRecomputeHasReachedRatherThanZeros() {
		storageRepo.seedAvatars(List.of("Brynja"));

		List<AvatarContribution> all = tested.ofEveryKnownAvatar().avatars();

		assertThat(all).containsExactly(new AvatarContribution("Brynja", Optional.empty(), null, null, null));
	}

	@ParameterizedTest(name = "answers no contribution while only the {0} of the four sums is missing")
	@MethodSource("eachOfTheFourSums")
	void answersNoContributionWhileAnyOneOfTheFourSumsIsMissing(String sum, MetaInformationKey<?> missing) {
		bankRepo.seedAvatars(List.of("Aurora"));
		recomputedWithout(missing, "Aurora");

		AvatarContribution aurora = tested.ofEveryKnownAvatar().avatars().getFirst();

		assertThat(aurora.contribution()).isEmpty();
	}

	static Stream<Arguments> eachOfTheFourSums() {
		return Stream
				.of(Arguments.of("bank placement", getBankPlacement("Aurora")), Arguments.of("bank withdrawal", getBankWithdrawl("Aurora")),
						Arguments.of("storage placement", getStoragePlacement("Aurora")), Arguments.of("storage withdrawal", getStorageWithdrawl("Aurora")));
	}

	@Test
	void answersNoContributionWhileOneOfTheFourSumsIsMissingRatherThanCountingItAsZero() {
		bankRepo.seedAvatars(List.of("Aurora"));
		metaRepo.put(getBankPlacement("Aurora"), 1500L);
		metaRepo.put(getBankWithdrawl("Aurora"), 200L);
		metaRepo.put(getStoragePlacement("Aurora"), 185.04);

		AvatarContribution aurora = tested.ofEveryKnownAvatar().avatars().getFirst();

		assertThat(aurora.contribution()).isEmpty();
	}

	@Test
	void reportsNoStaleSumsForAnAvatarNoRecomputeHasReachedEvenWhenAnOlderInstantIsStoredForHim() {
		bankRepo.seedAvatars(List.of("Aurora", "Brynja"));
		metaRepo.put(getSumsRecomputedAt("Aurora"), EARLIER);
		recomputed("Brynja", LATER);

		List<AvatarContribution> all = tested.ofEveryKnownAvatar().avatars();

		assertThat(all).extracting(AvatarContribution::avatar, AvatarContribution::staleSumsFrom).containsExactly(tuple("Aurora", null), tuple("Brynja", null));
	}

	@Test
	void datesTheLastCollectionByTheComputedAvatarsAloneSoThatAnInstantWithoutSumsMarksNobodyStale() {
		bankRepo.seedAvatars(List.of("Aurora", "Brynja"));
		metaRepo.put(getSumsRecomputedAt("Aurora"), LATER);
		recomputed("Brynja", EARLIER);

		List<AvatarContribution> all = tested.ofEveryKnownAvatar().avatars();

		assertThat(all).extracting(AvatarContribution::avatar, AvatarContribution::staleSumsFrom).containsExactly(tuple("Aurora", null), tuple("Brynja", null));
	}

	@Test
	void totalsNothingWhileOneAvatarIsNotYetComputedRatherThanLeavingHimOut() {
		bankRepo.seedAvatars(List.of("Aurora", "Brynja"));
		recomputed("Aurora", LATER);

		assertThat(tested.ofEveryKnownAvatar().total()).isEmpty();
	}

	@Test
	void totalsTheContributionsOfEveryAvatarWhileAllOfThemAreComputed() {
		bankRepo.seedAvatars(List.of("Aurora", "Brynja"));
		recomputed("Aurora", LATER);
		recomputed("Brynja", LATER);
		metaRepo.put(getBankPlacement("Aurora"), 1500L);
		metaRepo.put(getStorageWithdrawl("Brynja"), 300.0);

		assertThat(tested.ofEveryKnownAvatar().total()).contains(new Contribution(1500, 0, 0, 300.0, Optional.empty()));
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

	private void recomputed(String avatar, Instant at) {
		metaRepo.put(getBankPlacement(avatar), 0L);
		metaRepo.put(getBankWithdrawl(avatar), 0L);
		metaRepo.put(getStoragePlacement(avatar), 0.0);
		metaRepo.put(getStorageWithdrawl(avatar), 0.0);
		metaRepo.put(getSumsRecomputedAt(avatar), at);
	}

	private void recomputedWithout(MetaInformationKey<?> missing, String avatar) {
		List<MetaInformation<?>> sums = List
				.of(new MetaInformation<>(getBankPlacement(avatar), 0L), new MetaInformation<>(getBankWithdrawl(avatar), 0L),
						new MetaInformation<>(getStoragePlacement(avatar), 0.0), new MetaInformation<>(getStorageWithdrawl(avatar), 0.0));
		metaRepo.add(sums.stream().filter(sum -> !sum.key().id.equals(missing.id)).toList());
		metaRepo.put(getSumsRecomputedAt(avatar), LATER);
	}
}
