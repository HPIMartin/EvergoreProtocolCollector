package dev.schoenberg.evergore.protocolParser.application;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LoginGenerationsTest {
	private final LoginGenerations tested = new LoginGenerations();

	@Test
	void startsEveryAccountAtGenerationZero() {
		long generation = tested.of("aurora");

		assertThat(generation).isZero();
	}

	@Test
	void raisesOnlyTheGenerationOfTheAccountItBumps() {
		tested.bump("aurora");

		long bumped = tested.of("aurora");
		long untouched = tested.of("boreas");

		assertThat(bumped).isEqualTo(1);
		assertThat(untouched).isZero();
	}

	@Test
	void raisesTheGenerationByOneForEveryBump() {
		tested.bump("aurora");
		tested.bump("aurora");

		long generation = tested.of("aurora");

		assertThat(generation).isEqualTo(2);
	}
}
