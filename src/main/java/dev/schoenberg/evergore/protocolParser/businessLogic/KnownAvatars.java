package dev.schoenberg.evergore.protocolParser.businessLogic;

import java.util.List;
import java.util.stream.Stream;

import dev.schoenberg.evergore.protocolParser.businessLogic.banking.BankRepository;
import dev.schoenberg.evergore.protocolParser.businessLogic.storage.StorageRepository;

public class KnownAvatars {
	private final BankRepository bankRepo;
	private final StorageRepository storageRepo;

	public KnownAvatars(BankRepository bankRepo, StorageRepository storageRepo) {
		this.bankRepo = bankRepo;
		this.storageRepo = storageRepo;
	}

	public List<String> sortedByName() {
		return Stream.concat(bankRepo.getAllDifferentAvatars().stream(), storageRepo.getAllDifferentAvatars().stream()).distinct().sorted().toList();
	}
}
