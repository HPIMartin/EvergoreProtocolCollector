package dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation;

public record MetaInformation<T>(MetaInformationKey<T> key, T value) {

	public String getSerializedValue() {
		return key.serialize(value);
	}

	public static <T> MetaInformation<T> fromSerializedValue(MetaInformationKey<T> key, String raw) {
		return new MetaInformation<>(key, key.deserialize(raw));
	}
}
