package dev.schoenberg.evergore.protocolParser.businessLogic.contribution;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public record GuildContributions(Optional<LocalDateTime> lastUpdated, List<AvatarContribution> avatars) {
	public boolean containsStaleSums() {
		return avatars.stream().anyMatch(avatar -> avatar.staleSumsFrom() != null);
	}
}
