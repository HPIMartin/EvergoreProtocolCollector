package dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation;

import java.util.Map;
import java.util.Optional;

public record MetaInformationSnapshot(Map<String, String> serializedValues) {
	public MetaInformationSnapshot(Map<String, String> serializedValues) {
		this.serializedValues = Map.copyOf(serializedValues);
	}

	public <T> Optional<T> get(MetaInformationKey<T> key) {
		return Optional.ofNullable(serializedValues.get(key.id)).map(key::deserialize);
	}
}
