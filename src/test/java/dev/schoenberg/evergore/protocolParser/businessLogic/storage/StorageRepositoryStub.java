package dev.schoenberg.evergore.protocolParser.businessLogic.storage;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Comparator.naturalOrder;
import static java.util.stream.Collectors.toMap;

public class StorageRepositoryStub implements StorageRepository {
	private final Map<String, List<StorageEntry>> entriesByAvatar = new HashMap<>();
	private List<String> avatars = new ArrayList<>();

	public void seedEntries(String avatar, List<StorageEntry> entries) {
		entriesByAvatar.put(avatar, entries);
	}

	public void seedAvatars(List<String> list) {
		avatars = list;
	}

	@Override
	public List<StorageEntry> getAllFor(String avatar) {
		return entriesByAvatar.getOrDefault(avatar, List.of());
	}

	@Override
	public long countFor(String avatar) {
		return getAllFor(avatar).size();
	}

	@Override
	public List<String> getAllDifferentAvatars() {
		return avatars;
	}

	@Override
	public Map<String, Instant> latestTimestampPerAvatar() {
		return entriesByAvatar
				.entrySet()
				.stream()
				.filter(byAvatar -> !byAvatar.getValue().isEmpty())
				.collect(toMap(Map.Entry::getKey, byAvatar -> byAvatar.getValue().stream().map(StorageEntry::timeStamp).max(naturalOrder()).orElseThrow()));
	}

	@Override
	public void add(List<StorageEntry> newEntries) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<StorageEntry> getAllFor(String avatar, long page, long size) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<StorageEntry> getAllSince(Instant timestampInclusive) {
		throw new UnsupportedOperationException();
	}
}
