package dev.schoenberg.evergore.protocolParser.businessLogic.storage;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public interface StorageRepository {
	void add(List<StorageEntry> newEntries);

	List<StorageEntry> getAllFor(String avatar, long page, long size);

	List<StorageEntry> getAllFor(String avatar);

	long countFor(String avatar);

	List<StorageEntry> getAllSince(Instant timestampInclusive);

	List<String> getAllDifferentAvatars();

	Map<String, Instant> latestTimestampPerAvatar();
}
