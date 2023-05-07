package dev.schoenberg.evergore.protocolParser.businessLogic.storage;

import java.util.*;

public interface StorageRepository {
	void add(List<StorageEntry> newEntries);

	List<StorageEntry> getAllFor(String avatar, long page, long size);

	StorageEntry getNewest();

	List<String> getAllDifferentAvatars();
}
