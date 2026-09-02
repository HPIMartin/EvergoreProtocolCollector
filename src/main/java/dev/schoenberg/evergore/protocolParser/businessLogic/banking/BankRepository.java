package dev.schoenberg.evergore.protocolParser.businessLogic.banking;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public interface BankRepository {
	void add(List<BankEntry> newEntries);

	List<BankEntry> getAllFor(String avatar, long page, long size);

	List<BankEntry> getAllFor(String avatar);

	long countFor(String avatar);

	List<BankEntry> getAllSince(Instant timestampInclusive);

	List<String> getAllDifferentAvatars();

	Map<String, Instant> latestTimestampPerAvatar();
}
