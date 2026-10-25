package dev.schoenberg.evergore.protocolParser.application;

import dev.schoenberg.evergore.protocolParser.businessLogic.account.Account;
import dev.schoenberg.evergore.protocolParser.businessLogic.banking.BankRepository;
import dev.schoenberg.evergore.protocolParser.businessLogic.contribution.ActivityLine;
import dev.schoenberg.evergore.protocolParser.businessLogic.storage.StorageRepository;

public class RestingAccounts {
	private final BankRepository bankRepository;
	private final StorageRepository storageRepository;

	public RestingAccounts(BankRepository bankRepository, StorageRepository storageRepository) {
		this.bankRepository = bankRepository;
		this.storageRepository = storageRepository;
	}

	public boolean rest(Account account) {
		ActivityLine activityLine = ActivityLine.of(bankRepository.latestTimestampPerAvatar(), storageRepository.latestTimestampPerAvatar());

		return !activityLine.isActive(account.avatar());
	}
}
