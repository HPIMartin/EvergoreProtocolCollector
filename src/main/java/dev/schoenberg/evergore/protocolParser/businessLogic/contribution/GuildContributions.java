package dev.schoenberg.evergore.protocolParser.businessLogic.contribution;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public record GuildContributions(Optional<Instant> lastUpdated, List<AvatarContribution> avatars) {
	public Optional<Contribution> total() {
		if (avatars.stream().anyMatch(avatar -> avatar.contribution().isEmpty())) {
			return Optional.empty();
		}

		return Optional.of(Contribution.sumOf(avatars.stream().map(avatar -> avatar.contribution().orElseThrow()).toList()));
	}

	public boolean containsStaleSums() {
		return avatars.stream().anyMatch(avatar -> avatar.staleSumsFrom() != null);
	}
}
