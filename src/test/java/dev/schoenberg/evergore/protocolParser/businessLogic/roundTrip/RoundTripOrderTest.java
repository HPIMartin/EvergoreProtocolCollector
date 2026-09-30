package dev.schoenberg.evergore.protocolParser.businessLogic.roundTrip;

import java.util.List;

import org.junit.jupiter.api.Test;

import static dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.ACHAT_ARMBRUST;
import static dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.FEDERN;
import static org.assertj.core.api.Assertions.assertThat;

class RoundTripOrderTest {
	@Test
	void ordersRoundTripsByAvatarInGermanOrderThenByItem() {
		RoundTrip zornFeathers = new RoundTrip("Zorn", FEDERN, 1);
		RoundTrip aergerFeathers = new RoundTrip("Ärger", FEDERN, 1);
		RoundTrip aergerCrossbow = new RoundTrip("Ärger", ACHAT_ARMBRUST, 1);

		List<RoundTrip> sorted = List.of(zornFeathers, aergerFeathers, aergerCrossbow).stream().sorted(RoundTripOrder.TRIPS).toList();

		assertThat(sorted).containsExactly(aergerCrossbow, aergerFeathers, zornFeathers);
	}

	@Test
	void ordersAbstentionsByAvatarInGermanOrderThenByItem() {
		RoundTripAbstention zornFeathers = new RoundTripAbstention("Zorn", FEDERN);
		RoundTripAbstention aergerFeathers = new RoundTripAbstention("Ärger", FEDERN);
		RoundTripAbstention aergerCrossbow = new RoundTripAbstention("Ärger", ACHAT_ARMBRUST);

		List<RoundTripAbstention> sorted = List.of(zornFeathers, aergerFeathers, aergerCrossbow).stream().sorted(RoundTripOrder.ABSTENTIONS).toList();

		assertThat(sorted).containsExactly(aergerCrossbow, aergerFeathers, zornFeathers);
	}
}
