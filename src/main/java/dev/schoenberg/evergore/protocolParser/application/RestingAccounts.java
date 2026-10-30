package dev.schoenberg.evergore.protocolParser.application;

import java.util.function.Predicate;

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
		return forOneListing().test(account);
	}

	public Predicate<Account> forOneListing() {
		ActivityLine activityLine = ActivityLine.of(bankRepository.latestTimestampPerAvatar(), storageRepository.latestTimestampPerAvatar());

		return account -> !activityLine.isActive(account.avatar());
	}
}
