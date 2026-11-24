package dev.schoenberg.evergore.protocolParser.application;

import java.time.Duration;

public record UsageWriting(int capacity, int batchSize, Duration closeBound) {}
