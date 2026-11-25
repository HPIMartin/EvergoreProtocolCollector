package dev.schoenberg.evergore.protocolParser.application;

import java.time.LocalDate;
import java.util.Optional;

public record UsageReport(UsageWindow lastThirtyDays, UsageWindow allTheTime, Optional<LocalDate> recordedSince, long lostSinceStart) {}
