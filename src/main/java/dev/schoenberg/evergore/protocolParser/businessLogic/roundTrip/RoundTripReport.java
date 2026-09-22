package dev.schoenberg.evergore.protocolParser.businessLogic.roundTrip;

import java.util.List;

public record RoundTripReport(List<RoundTrip> roundTrips, List<RoundTripAbstention> abstentions) {}
