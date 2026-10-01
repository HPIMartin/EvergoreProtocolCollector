package dev.schoenberg.evergore.protocolParser.businessLogic.roundTrip;

import dev.schoenberg.evergore.protocolParser.domain.EvergoreItem;

public record RoundTrip(String avatar, EvergoreItem item, int quantity) {
	public RoundTrip {
		if (quantity <= 0) {
			throw new IllegalArgumentException("A round trip moves at least one piece, not " + quantity);
		}
	}
}
