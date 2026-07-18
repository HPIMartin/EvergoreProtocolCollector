package dev.schoenberg.evergore.protocolParser.businessLogic.storage;

import java.time.Instant;
import java.util.List;

public interface StorageRepository {
	void add(List<StorageEntry> newEntries);

	List<StorageEntry> getAllFor(String avatar, long page, long size);

	List<StorageEntry> getAllFor(String avatar);

	List<StorageEntry> getAllSince(Instant timestampInclusive);

	List<String> getAllDifferentAvatars();
}
