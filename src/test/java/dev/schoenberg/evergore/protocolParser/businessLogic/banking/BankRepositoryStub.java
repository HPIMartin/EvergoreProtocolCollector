package dev.schoenberg.evergore.protocolParser.businessLogic.banking;

import dev.schoenberg.evergore.protocolParser.businessLogic.base.LedgerRepositoryStub;

public class BankRepositoryStub extends LedgerRepositoryStub<BankEntry> implements BankRepository {
	public BankRepositoryStub() {
		super(BankEntry::timeStamp);
	}
}
