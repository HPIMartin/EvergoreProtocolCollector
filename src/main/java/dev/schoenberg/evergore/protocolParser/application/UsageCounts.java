package dev.schoenberg.evergore.protocolParser.application;

import java.time.Instant;
import java.util.Optional;

public record UsageCounts(int logins, int pageLoads, int logouts, int daysUsed, Optional<Instant> latestUse) {}
