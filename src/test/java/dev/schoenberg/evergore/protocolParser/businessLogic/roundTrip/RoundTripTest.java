package dev.schoenberg.evergore.protocolParser.businessLogic.roundTrip;

import org.junit.jupiter.api.Test;

import static dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.FEDERN;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoundTripTest {
	@Test
	void refusesARoundTripOfNothing() {
		assertThatThrownBy(() -> new RoundTrip("Alrik", FEDERN, 0)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void refusesARoundTripOfANegativeQuantity() {
		assertThatThrownBy(() -> new RoundTrip("Alrik", FEDERN, -5)).isInstanceOf(IllegalArgumentException.class);
	}
}
