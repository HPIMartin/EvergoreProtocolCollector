package dev.schoenberg.evergore.protocolParser.businessLogic.storage;

import dev.schoenberg.evergore.protocolParser.businessLogic.base.LedgerRepository;

public interface StorageRepository extends LedgerRepository<StorageEntry, StorageSortKey> {}
