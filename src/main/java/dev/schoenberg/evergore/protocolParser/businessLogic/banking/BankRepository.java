package dev.schoenberg.evergore.protocolParser.businessLogic.banking;

import java.util.List;
import java.util.Optional;

public interface BankRepository {
	void add(List<BankEntry> newEntries);

	List<BankEntry> getAllFor(String avatar, long page, long size);

	List<BankEntry> getAllFor(String avatar);

	Optional<BankEntry> getNewest();

	List<String> getAllDifferentAvatars();
}
