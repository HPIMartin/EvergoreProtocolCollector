package dev.schoenberg.evergore.protocolParser.businessLogic.roundTrip;

import dev.schoenberg.evergore.protocolParser.businessLogic.storage.StorageEntry;
import dev.schoenberg.evergore.protocolParser.domain.EvergoreItem;

public record ResolvedStorageEntry(StorageEntry entry, EvergoreItem item) {}
