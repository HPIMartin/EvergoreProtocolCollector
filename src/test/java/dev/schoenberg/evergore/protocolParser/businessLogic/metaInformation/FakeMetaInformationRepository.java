package dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class FakeMetaInformationRepository implements MetaInformationRepository {
	private final Map<String, String> store = new HashMap<>();
	private final List<List<String>> writtenBatches = new ArrayList<>();
	private int takenSnapshots;

	@Override
	public MetaInformationSnapshot snapshot() {
		takenSnapshots++;
		return new MetaInformationSnapshot(store);
	}

	@Override
	public void add(List<? extends MetaInformation<?>> meta) {
		writtenBatches.add(meta.stream().map(m -> m.key().id).toList());
		for (MetaInformation<?> m : meta) {
			store.put(m.key().id, m.getSerializedValue());
		}
	}

	public <T> void put(MetaInformationKey<T> key, T value) {
		store.put(key.id, key.serialize(value));
	}

	public <T> Optional<T> get(MetaInformationKey<T> key) {
		return new MetaInformationSnapshot(store).get(key);
	}

	public List<List<String>> writtenBatches() {
		return List.copyOf(writtenBatches);
	}

	public int takenSnapshots() {
		return takenSnapshots;
	}
}
