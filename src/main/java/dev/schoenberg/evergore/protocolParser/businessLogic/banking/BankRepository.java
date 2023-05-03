package dev.schoenberg.evergore.protocolParser.businessLogic.banking;

import java.util.*;

public interface BankRepository {
	void add(List<BankEntry> newEntries);

	List<BankEntry> getAllFor(String avatar, long page, long size);

	BankEntry getNewest();
}
