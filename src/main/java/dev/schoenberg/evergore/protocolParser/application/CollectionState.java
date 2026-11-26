package dev.schoenberg.evergore.protocolParser.application;

import java.time.Instant;
import java.util.Optional;

public record CollectionState(Optional<Instant> runningSince, Optional<Instant> nextStartByHandFrom) {}
