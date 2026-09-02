package dev.schoenberg.evergore.protocolParser.businessLogic.contribution;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import dev.schoenberg.evergore.protocolParser.businessLogic.KnownAvatars;
import dev.schoenberg.evergore.protocolParser.businessLogic.banking.BankRepository;
import dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationRepository;
import dev.schoenberg.evergore.protocolParser.businessLogic.storage.StorageRepository;

import static dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey.getBankPlacement;
import static dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey.getBankWithdrawl;
import static dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey.getStoragePlacement;
import static dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey.getStorageWithdrawl;

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

	public List<AvatarContribution> ofEveryKnownAvatar() {
		Map<String, Instant> lastBankActivity = bankRepo.latestTimestampPerAvatar();
		Map<String, Instant> lastStorageActivity = storageRepo.latestTimestampPerAvatar();

		return knownAvatars.sortedByName().stream().map(avatar -> contributionOf(avatar, lastBankActivity.get(avatar), lastStorageActivity.get(avatar))).toList();
	}

	private AvatarContribution contributionOf(String avatar, Instant lastBankActivity, Instant lastStorageActivity) {
		Contribution contribution = new Contribution(metaRepo.get(getBankPlacement(avatar)).orElse(0L), metaRepo.get(getBankWithdrawl(avatar)).orElse(0L),
				metaRepo.get(getStoragePlacement(avatar)).orElse(0D), metaRepo.get(getStorageWithdrawl(avatar)).orElse(0D));

		return new AvatarContribution(avatar, contribution, lastBankActivity, lastStorageActivity);
	}
}
