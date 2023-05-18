package dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation;

public class MetaInformation<T> {
	public final MetaInformationKey<T> key;
	public final T value;

	public MetaInformation(MetaInformationKey<T> key, T value) {
		this.key = key;
		this.value = value;
	}

	public String getSerializedValue() {
		return key.serialize(value);
	}

	public static <T> MetaInformation<T> fromSerializedValue(MetaInformationKey<T> key, String raw) {
		return new MetaInformation<>(key, key.deserialize(raw));
	}
}
