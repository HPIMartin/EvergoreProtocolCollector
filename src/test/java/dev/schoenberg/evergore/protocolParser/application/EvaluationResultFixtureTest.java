package dev.schoenberg.evergore.protocolParser.application;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import dev.schoenberg.evergore.protocolParser.businessLogic.roundTrip.RoundTrip;
import dev.schoenberg.evergore.protocolParser.businessLogic.roundTrip.RoundTripAbstention;

import static dev.schoenberg.evergore.protocolParser.domain.EvergoreItem.FEDERN;
import static org.assertj.core.api.Assertions.assertThat;

class EvaluationResultFixtureTest {
	private final EvaluationResultFixture tested = new EvaluationResultFixture();

	@Test
	void passesTheUnknownItemNamesThroughUnchanged() {
		List<String> unknownItemNames = new ArrayList<>(List.of("unknown"));

		EvaluationResult result = tested.unknownItemNames(unknownItemNames).build();

		assertThat(result.unknownItemNames()).isSameAs(unknownItemNames);
	}

	@Test
	void passesTheZeroValuedItemNamesThroughUnchanged() {
		List<String> zeroValuedItemNames = new ArrayList<>(List.of("zero-valued"));

		EvaluationResult result = tested.zeroValuedItemNames(zeroValuedItemNames).build();

		assertThat(result.zeroValuedItemNames()).isSameAs(zeroValuedItemNames);
	}

	@Test
	void passesTheFailedAvatarNamesThroughUnchanged() {
		List<String> failedAvatarNames = new ArrayList<>(List.of("failed"));

		EvaluationResult result = tested.failedAvatarNames(failedAvatarNames).build();

		assertThat(result.failedAvatarNames()).isSameAs(failedAvatarNames);
	}

	@Test
	void passesTheRoundTripsThroughUnchanged() {
		List<RoundTrip> roundTrips = new ArrayList<>(List.of(new RoundTrip("Alrik", FEDERN, 1)));

		EvaluationResult result = tested.roundTrips(roundTrips).build();

		assertThat(result.roundTrips()).isSameAs(roundTrips);
	}

	@Test
	void passesTheRoundTripAbstentionsThroughUnchanged() {
		List<RoundTripAbstention> roundTripAbstentions = new ArrayList<>(List.of(new RoundTripAbstention("Alrik", FEDERN)));

		EvaluationResult result = tested.roundTripAbstentions(roundTripAbstentions).build();

		assertThat(result.roundTripAbstentions()).isSameAs(roundTripAbstentions);
	}
}
