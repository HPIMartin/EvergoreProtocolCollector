package dev.schoenberg.evergore.protocolParser.businessLogic.banking;

import java.time.Instant;

import dev.schoenberg.evergore.protocolParser.businessLogic.base.TransferType;

public record BankEntry(Instant timeStamp, String avatar, int amount, TransferType type) {}
