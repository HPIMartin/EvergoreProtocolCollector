package dev.schoenberg.evergore.protocolParser.application;

import java.util.List;

import dev.schoenberg.evergore.protocolParser.businessLogic.roundTrip.RoundTrip;
import dev.schoenberg.evergore.protocolParser.businessLogic.roundTrip.RoundTripAbstention;

public record EvaluationResult(List<String> unknownItemNames, List<String> zeroValuedItemNames, List<String> failedAvatarNames, List<RoundTrip> roundTrips,
		List<RoundTripAbstention> roundTripAbstentions) {}
