package dev.schoenberg.evergore.protocolParser.businessLogic.usage;

import java.time.Instant;
import java.util.Optional;

public record UsageRecord(String accountKey, Instant at, UsageKind kind, Optional<String> detail) {}
