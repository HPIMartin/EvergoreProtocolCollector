package dev.schoenberg.evergore.protocolParser.application;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;
import java.util.concurrent.atomic.AtomicReference;

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
		tested.bumpWhileAt("aurora", 0, () -> {});

		long bumped = tested.of("aurora");
		long untouched = tested.of("boreas");

		assertThat(bumped).isEqualTo(1);
		assertThat(untouched).isZero();
	}

	@Test
	void runsTheChangeAndRaisesTheGenerationWhileItIsStillTheExpectedOne() {
		List<String> trail = new ArrayList<>();

		OptionalLong bumped = tested.bumpWhileAt("aurora", 0, () -> trail.add("changed"));

		long generation = tested.of("aurora");

		assertThat(bumped).hasValue(1);
		assertThat(trail).containsExactly("changed");
		assertThat(generation).isEqualTo(1);
	}

	@Test
	void runsNoChangeAndRaisesNothingOnceAnotherGenerationIsCurrent() {
		List<String> trail = new ArrayList<>();
		tested.bumpWhileAt("aurora", 0, () -> {});

		OptionalLong bumped = tested.bumpWhileAt("aurora", 0, () -> trail.add("changed"));

		long generation = tested.of("aurora");

		assertThat(bumped).isEmpty();
		assertThat(trail).isEmpty();
		assertThat(generation).isEqualTo(1);
	}

	@Test
	void holdsASecondBumpOfTheSameAccountUntilTheFirstChangeIsDone() throws InterruptedException {
		AtomicReference<OptionalLong> second = new AtomicReference<>();
		Thread[] rival = new Thread[1];
		Thread.State[] seen = new Thread.State[1];

		OptionalLong first = tested.bumpWhileAt("aurora", 0, () -> {
			rival[0] = new Thread(() -> second.set(tested.bumpWhileAt("aurora", 0, () -> {})));
			rival[0].start();
			seen[0] = stateOnceItBlocksOrEnds(rival[0]);
		});
		rival[0].join();

		long generation = tested.of("aurora");

		assertThat(seen[0]).isNotEqualTo(Thread.State.TERMINATED);
		assertThat(first).hasValue(1);
		assertThat(second.get()).isEmpty();
		assertThat(generation).isEqualTo(1);
	}

	private static Thread.State stateOnceItBlocksOrEnds(Thread thread) {
		Thread.State state = thread.getState();
		while (state != Thread.State.BLOCKED && state != Thread.State.WAITING && state != Thread.State.TERMINATED) {
			Thread.onSpinWait();
			state = thread.getState();
		}
		return state;
	}

	@Test
	void raisesTheGenerationByOneForEveryBump() {
		tested.bumpWhileAt("aurora", 0, () -> {});
		tested.bumpWhileAt("aurora", 1, () -> {});

		long generation = tested.of("aurora");

		assertThat(generation).isEqualTo(2);
	}
}
