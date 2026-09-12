package dev.schoenberg.evergore.protocolParser.application;

import java.util.List;

public record EvaluationResult(List<String> unknownItemNames, List<String> zeroValuedItemNames, List<String> failedAvatarNames) {}
