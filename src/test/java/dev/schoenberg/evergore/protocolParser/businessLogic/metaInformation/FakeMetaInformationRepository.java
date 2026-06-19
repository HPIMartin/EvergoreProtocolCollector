package dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class FakeMetaInformationRepository implements MetaInformationRepository {
	private final Map<String, Object> store = new HashMap<>();

	@SuppressWarnings("unchecked")
	@Override
	public <T> Optional<T> get(MetaInformationKey<T> key) {
		return Optional.ofNullable((T) store.get(key.id));
	}

	@Override
	public <T> void add(List<MetaInformation<T>> meta) {
		for (MetaInformation<T> m : meta) {
			store.put(m.key.id, m.value);
		}
	}

	public <T> void put(MetaInformationKey<T> key, T value) {
		store.put(key.id, value);
	}
}
