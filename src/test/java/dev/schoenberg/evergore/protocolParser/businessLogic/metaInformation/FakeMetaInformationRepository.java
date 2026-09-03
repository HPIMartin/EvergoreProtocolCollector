package dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class FakeMetaInformationRepository implements MetaInformationRepository {
	private final Map<String, Object> store = new HashMap<>();
	private final List<List<String>> writtenBatches = new ArrayList<>();

	@SuppressWarnings("unchecked")
	@Override
	public <T> Optional<T> get(MetaInformationKey<T> key) {
		return Optional.ofNullable((T) store.get(key.id));
	}

	@Override
	public void add(List<? extends MetaInformation<?>> meta) {
		writtenBatches.add(meta.stream().map(m -> m.key().id).toList());
		for (MetaInformation<?> m : meta) {
			store.put(m.key().id, m.value());
		}
	}

	public List<List<String>> writtenBatches() {
		return List.copyOf(writtenBatches);
	}

	public <T> void put(MetaInformationKey<T> key, T value) {
		store.put(key.id, value);
	}
}
