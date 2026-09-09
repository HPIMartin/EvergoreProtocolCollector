package dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import static dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey.getSumsRecomputedAt;
import static java.util.Comparator.naturalOrder;

public record MetaInformationSnapshot(Map<String, String> serializedValues) {
	public MetaInformationSnapshot(Map<String, String> serializedValues) {
		this.serializedValues = Map.copyOf(serializedValues);
	}

	public <T> Optional<T> get(MetaInformationKey<T> key) {
		return Optional.ofNullable(serializedValues.get(key.id)).map(key::deserialize);
	}

	public Optional<Instant> lastRecomputeOf(Collection<String> avatars) {
		return avatars.stream().map(avatar -> get(getSumsRecomputedAt(avatar))).flatMap(Optional::stream).max(naturalOrder());
	}
}
