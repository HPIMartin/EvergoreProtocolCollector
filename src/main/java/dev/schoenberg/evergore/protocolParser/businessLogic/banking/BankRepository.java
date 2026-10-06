package dev.schoenberg.evergore.protocolParser.businessLogic.banking;

import dev.schoenberg.evergore.protocolParser.businessLogic.base.LedgerRepository;

public interface BankRepository extends LedgerRepository<BankEntry, BankSortKey> {}
