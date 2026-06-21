package dev.schoenberg.evergore.protocolParser.businessLogic.storage;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface StorageRepository {
	void add(List<StorageEntry> newEntries);

	List<StorageEntry> getAllFor(String avatar, long page, long size);

	List<StorageEntry> getAllFor(String avatar, LocalDateTime after);

	Optional<StorageEntry> getNewest();

	List<String> getAllDifferentAvatars();
}
