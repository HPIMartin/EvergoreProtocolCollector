package dev.schoenberg.evergore.protocolParser.businessLogic.banking;

import java.time.LocalDateTime;
import java.util.List;

public interface BankRepository {
	void add(List<BankEntry> newEntries);

	List<BankEntry> getAllFor(String avatar, long page, long size);

	List<BankEntry> getAllFor(String avatar, LocalDateTime after);

	BankEntry getNewest();

	List<String> getAllDifferentAvatars();
}
