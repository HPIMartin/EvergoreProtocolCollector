package dev.schoenberg.evergore.protocolParser.businessLogic.contribution;

import java.util.List;

import dev.schoenberg.evergore.protocolParser.businessLogic.KnownAvatars;
import dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationRepository;

import static dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey.getBankPlacement;
import static dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey.getBankWithdrawl;
import static dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey.getStoragePlacement;
import static dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey.getStorageWithdrawl;

public class AvatarContributions {
	private final KnownAvatars knownAvatars;
	private final MetaInformationRepository metaRepo;

	public AvatarContributions(KnownAvatars knownAvatars, MetaInformationRepository metaRepo) {
		this.knownAvatars = knownAvatars;
		this.metaRepo = metaRepo;
	}

	public List<AvatarContribution> ofEveryKnownAvatar() {
		return knownAvatars.sortedByName().stream().map(this::contributionOf).toList();
	}

	private AvatarContribution contributionOf(String avatar) {
		Contribution contribution = new Contribution(metaRepo.get(getBankPlacement(avatar)).orElse(0L), metaRepo.get(getBankWithdrawl(avatar)).orElse(0L),
				metaRepo.get(getStoragePlacement(avatar)).orElse(0D), metaRepo.get(getStorageWithdrawl(avatar)).orElse(0D));

		return new AvatarContribution(avatar, contribution);
	}
}
