package dev.schoenberg.evergore.protocolParser.businessLogic.storage;

import java.time.LocalDateTime;
import java.util.List;

public interface StorageRepository {
	void add(List<StorageEntry> newEntries);

	List<StorageEntry> getAllFor(String avatar, long page, long size);

	List<StorageEntry> getAllFor(String avatar, LocalDateTime after);

	StorageEntry getNewest();

	List<String> getAllDifferentAvatars();
}
