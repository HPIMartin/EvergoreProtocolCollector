package dev.schoenberg.evergore.protocolParser.businessLogic.base;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public interface LedgerRepository<E> {
	void add(List<E> newEntries);

	List<E> getAllFor(String avatar, long page, long size);

	List<E> getAllFor(String avatar);

	long countFor(String avatar);

	List<E> getAllSince(Instant timestampInclusive);

	List<String> getAllDifferentAvatars();

	Map<String, Instant> latestTimestampPerAvatar();
}
