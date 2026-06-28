package dev.schoenberg.evergore.protocolParser.businessLogic.storage;

import java.time.Instant;

import dev.schoenberg.evergore.protocolParser.businessLogic.base.TransferType;

public record StorageEntry(Instant timeStamp, String avatar, int quantity, String name, int quality, TransferType type) {}
