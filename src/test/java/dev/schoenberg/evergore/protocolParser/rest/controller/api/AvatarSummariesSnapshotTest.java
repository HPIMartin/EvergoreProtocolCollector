package dev.schoenberg.evergore.protocolParser.rest.controller.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import dev.schoenberg.evergore.protocolParser.LoggerSpy;
import dev.schoenberg.evergore.protocolParser.businessLogic.KnownAvatars;
import dev.schoenberg.evergore.protocolParser.businessLogic.banking.BankRepositoryStub;
import dev.schoenberg.evergore.protocolParser.businessLogic.contribution.AvatarContributions;
import dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformation;
import dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationRepository;
import dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationSnapshot;
import dev.schoenberg.evergore.protocolParser.businessLogic.storage.StorageRepositoryStub;
import dev.schoenberg.evergore.protocolParser.rest.controller.api.wire.AvatarSummary;
import dev.schoenberg.evergore.protocolParser.rest.controller.api.wire.AvatarSummaryPage;
import dev.schoenberg.evergore.protocolParser.rest.controller.api.wire.GuildTotals;

import static dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey.getBankPlacement;
import static dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey.getBankWithdrawl;
import static dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey.getStoragePlacement;
import static dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey.getStorageWithdrawl;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;

class AvatarSummariesSnapshotTest {
	private static final int WHOLE_PAGE = 100;
	private static final List<String> GUILD = List.of("Aurora", "Brynja", "Calix");

	private final BankRepositoryStub bankRepo = new BankRepositoryStub();
	private final StorageRepositoryStub storageRepo = new StorageRepositoryStub();
	private final RecomputingMetaInformationRepository recomputing = new RecomputingMetaInformationRepository(GUILD);

	@Test
	void answersEveryAvatarFromOneRecomputeStateWhileTheStoreIsBeingRewritten() {
		AvatarSummaryPage page = summariesReadDuringARecompute();

		assertThat(reportedSums(page)).hasSize(1);
	}

	@Test
	void answersAGuildTotalThatBelongsToTheSameRecomputeStateAsTheRows() {
		AvatarSummaryPage page = summariesReadDuringARecompute();
		GuildTotals totals = page.totals();
		long perAvatar = reportedSums(page).iterator().next();

		assertThat(totals.bankDeposited()).isEqualTo(perAvatar * GUILD.size());
		assertThat(totals.bankWithdrawn()).isEqualTo(perAvatar * GUILD.size());
		assertThat(totals.storageDeposited()).isEqualTo(perAvatar * GUILD.size());
		assertThat(totals.storageWithdrawn()).isEqualTo(perAvatar * GUILD.size());
	}

	private AvatarSummaryPage summariesReadDuringARecompute() {
		bankRepo.seedAvatars(GUILD);
		storageRepo.seedAvatars(List.of());
		AvatarContributions contributions = new AvatarContributions(new KnownAvatars(bankRepo, storageRepo), recomputing, bankRepo, storageRepo);

		return new AvatarSummariesController(contributions, new LoggerSpy()).summaries(0, WHOLE_PAGE);
	}

	private static Set<Long> reportedSums(AvatarSummaryPage page) {
		return page.items().stream().flatMap(AvatarSummariesSnapshotTest::sumsOf).collect(toSet());
	}

	private static Stream<Long> sumsOf(AvatarSummary summary) {
		return Stream.of(summary.bankDeposited(), summary.bankWithdrawn(), summary.storageDeposited(), summary.storageWithdrawn());
	}

	private static final class RecomputingMetaInformationRepository implements MetaInformationRepository {
		private final List<String> avatars;
		private long generation = 1;

		private RecomputingMetaInformationRepository(List<String> avatars) {
			this.avatars = avatars;
		}

		@Override
		public MetaInformationSnapshot snapshot() {
			MetaInformationSnapshot ofThisGeneration = new MetaInformationSnapshot(storeOfGeneration(generation));
			generation++;
			return ofThisGeneration;
		}

		@Override
		public void add(List<? extends MetaInformation<?>> meta) {
			throw new UnsupportedOperationException();
		}

		private Map<String, String> storeOfGeneration(long value) {
			Map<String, String> store = new HashMap<>();
			for (String avatar : avatars) {
				store.put(getBankPlacement(avatar).id, getBankPlacement(avatar).serialize(value));
				store.put(getBankWithdrawl(avatar).id, getBankWithdrawl(avatar).serialize(value));
				store.put(getStoragePlacement(avatar).id, getStoragePlacement(avatar).serialize((double) value));
				store.put(getStorageWithdrawl(avatar).id, getStorageWithdrawl(avatar).serialize((double) value));
			}
			return store;
		}
	}
}
