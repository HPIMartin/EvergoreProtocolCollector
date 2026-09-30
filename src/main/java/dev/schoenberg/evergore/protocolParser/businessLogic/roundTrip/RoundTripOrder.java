package dev.schoenberg.evergore.protocolParser.businessLogic.roundTrip;

import java.util.Comparator;

import dev.schoenberg.evergore.protocolParser.businessLogic.GermanOrder;

public final class RoundTripOrder {
	public static final Comparator<RoundTrip> TRIPS = Comparator.comparing(RoundTrip::avatar, GermanOrder.NAMES).thenComparing(trip -> trip.item().ingameName, GermanOrder.NAMES);
	public static final Comparator<RoundTripAbstention> ABSTENTIONS = Comparator
			.comparing(RoundTripAbstention::avatar, GermanOrder.NAMES)
			.thenComparing(abstention -> abstention.item().ingameName, GermanOrder.NAMES);

	private RoundTripOrder() {}
}
