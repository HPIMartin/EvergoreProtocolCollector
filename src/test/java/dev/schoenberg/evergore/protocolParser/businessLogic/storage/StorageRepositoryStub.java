package dev.schoenberg.evergore.protocolParser.businessLogic.storage;

import dev.schoenberg.evergore.protocolParser.businessLogic.base.LedgerRepositoryStub;

public class StorageRepositoryStub extends LedgerRepositoryStub<StorageEntry> implements StorageRepository {
	public StorageRepositoryStub() {
		super(StorageEntry::timeStamp);
	}
}
