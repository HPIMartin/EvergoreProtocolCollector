package dev.schoenberg.evergore.protocolParser.businessLogic.storage;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StorageRepositoryStub implements StorageRepository {
	private final Map<String, List<StorageEntry>> entriesByAvatar = new HashMap<>();
	private List<String> avatars = new ArrayList<>();
	private LocalDateTime capturedAfter;

	public void seedEntries(String avatar, List<StorageEntry> entries) {
		entriesByAvatar.put(avatar, entries);
	}

	public void seedAvatars(List<String> list) {
		avatars = list;
	}

	public LocalDateTime capturedAfter() {
		return capturedAfter;
	}

	@Override
	public List<StorageEntry> getAllFor(String avatar, LocalDateTime after) {
		capturedAfter = after;
		return entriesByAvatar.getOrDefault(avatar, List.of());
	}

	@Override
	public List<String> getAllDifferentAvatars() {
		return avatars;
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
	public StorageEntry getNewest() {
		throw new UnsupportedOperationException();
	}
}
