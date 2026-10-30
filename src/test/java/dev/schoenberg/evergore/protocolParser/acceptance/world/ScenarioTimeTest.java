package dev.schoenberg.evergore.protocolParser.acceptance.world;

import java.time.Instant;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import static dev.schoenberg.evergore.protocolParser.businessLogic.Constants.APP_ZONE;
import static org.assertj.core.api.Assertions.assertThat;

class ScenarioTimeTest {
	private static final LocalDateTime PINNED = LocalDateTime.of(2026, 12, 31, 12, 0);
	private static final LocalDateTime LATER = LocalDateTime.of(2026, 2, 1, 5, 0);

	private final ScenarioTime tested = new ScenarioTime();

	@Test
	void movesToAMomentUnlessPinnedWhenNothingWasPinned() {
		tested.setTo(PINNED);

		tested.setToUnlessPinned(LATER);

		Instant now = tested.now();
		assertThat(now).isEqualTo(LATER.atZone(APP_ZONE).toInstant());
	}

	@Test
	void staysAtThePinnedMomentWhenAskedToMoveUnlessPinned() {
		tested.pinTo(PINNED);

		tested.setToUnlessPinned(LATER);

		Instant now = tested.now();
		assertThat(now).isEqualTo(PINNED.atZone(APP_ZONE).toInstant());
	}

	@Test
	void movesWhenSetExplicitlyEvenIfPinned() {
		tested.pinTo(PINNED);

		tested.setTo(LATER);

		Instant now = tested.now();
		assertThat(now).isEqualTo(LATER.atZone(APP_ZONE).toInstant());
	}
}
