package dev.schoenberg.evergore.protocolParser.application;

import java.time.Instant;
import java.util.Optional;

public record AccountListing(String name, boolean resting, Optional<Instant> latestLogin) {}
