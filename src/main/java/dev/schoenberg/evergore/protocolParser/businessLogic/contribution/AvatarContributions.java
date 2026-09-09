package dev.schoenberg.evergore.protocolParser.businessLogic.contribution;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import dev.schoenberg.evergore.protocolParser.businessLogic.KnownAvatars;
import dev.schoenberg.evergore.protocolParser.businessLogic.banking.BankRepository;
import dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationRepository;
import dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationSnapshot;
import dev.schoenberg.evergore.protocolParser.businessLogic.storage.StorageRepository;

import static dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey.getBankPlacement;
import static dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey.getBankWithdrawl;
import static dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey.getLastUpdatedKey;
import static dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey.getStoragePlacement;
import static dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey.getStorageWithdrawl;
import static dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey.getSumsRecomputedAt;

public class AvatarContributions {
	private final KnownAvatars knownAvatars;
	private final MetaInformationRepository metaRepo;
	private final BankRepository bankRepo;
	private final StorageRepository storageRepo;

	public AvatarContributions(KnownAvatars knownAvatars, MetaInformationRepository metaRepo, BankRepository bankRepo, StorageRepository storageRepo) {
		this.knownAvatars = knownAvatars;
		this.metaRepo = metaRepo;
		this.bankRepo = bankRepo;
		this.storageRepo = storageRepo;
	}

	public GuildContributions ofEveryKnownAvatar() {
		MetaInformationSnapshot recompute = metaRepo.snapshot();
		Map<String, Instant> lastBankActivity = bankRepo.latestTimestampPerAvatar();
		Map<String, Instant> lastStorageActivity = storageRepo.latestTimestampPerAvatar();
		List<String> guild = knownAvatars.sortedByName();
		Optional<Instant> lastCollection = recompute.lastRecomputeOf(guild);

		List<AvatarContribution> avatars = guild
				.stream()
				.map(avatar -> contributionOf(recompute, avatar, lastBankActivity.get(avatar), lastStorageActivity.get(avatar), lastCollection))
				.toList();

		return new GuildContributions(recompute.get(getLastUpdatedKey()), avatars);
	}

	private static AvatarContribution contributionOf(MetaInformationSnapshot recompute, String avatar, Instant lastBankActivity, Instant lastStorageActivity,
			Optional<Instant> lastCollection) {
		Contribution contribution = new Contribution(recompute.get(getBankPlacement(avatar)).orElse(0L), recompute.get(getBankWithdrawl(avatar)).orElse(0L),
				recompute.get(getStoragePlacement(avatar)).orElse(0D), recompute.get(getStorageWithdrawl(avatar)).orElse(0D));

		return new AvatarContribution(avatar, contribution, lastBankActivity, lastStorageActivity, staleSumsFrom(recompute, avatar, lastCollection));
	}

	private static Instant staleSumsFrom(MetaInformationSnapshot recompute, String avatar, Optional<Instant> lastCollection) {
		Optional<Instant> recomputedAt = recompute.get(getSumsRecomputedAt(avatar));
		if (recomputedAt.isEmpty() || lastCollection.isEmpty()) {
			return null;
		}

		return recomputedAt.get().isBefore(lastCollection.get()) ? recomputedAt.get() : null;
	}
}
