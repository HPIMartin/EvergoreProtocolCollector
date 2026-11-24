package dev.schoenberg.evergore.protocolParser.application;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.Executor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.schoenberg.evergore.protocolParser.LoggerSpy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class CollectionsTest {
	private StubbedCollection collection;
	private HeldExecutor background;
	private LoggerSpy logger;
	private Collections tested;

	@BeforeEach
	void setup() {
		collection = new StubbedCollection();
		background = new HeldExecutor();
		logger = new LoggerSpy();
		tested = new Collections(collection, background, logger);
	}

	@Test
	void runsTheDailyCollectionThroughTheUseCase() {
		tested.runDaily();

		assertThat(collection.runs).isEqualTo(1);
	}

	@Test
	void logsTheStartOfTheDailyCollection() {
		tested.runDaily();

		assertThat(logger.infoMessages()).containsExactly("Daily collection started.");
	}

	@Test
	void leavesTheFailureOfTheDailyCollectionToTheScheduler() {
		collection.failure = new IllegalStateException("recompute failed");

		Throwable thrown = catchThrowable(() -> tested.runDaily());

		assertThat(thrown).isSameAs(collection.failure);
	}

	@Test
	void runsACollectionStartedByHandInTheBackgroundAndNotInTheCaller() {
		tested.startByHand();
		int runsInTheCaller = collection.runs;

		background.runHeldTasks();

		assertThat(runsInTheCaller).isZero();
		assertThat(collection.runs).isEqualTo(1);
	}

	@Test
	void logsTheStartByHandWhenItIsAccepted() {
		tested.startByHand();

		assertThat(logger.infoMessages()).containsExactly("Collection started by hand.");
	}

	private static class HeldExecutor implements Executor {
		private final Deque<Runnable> held = new ArrayDeque<>();

		@Override
		public void execute(Runnable task) {
			held.add(task);
		}

		void runHeldTasks() {
			while (!held.isEmpty()) {
				held.poll().run();
			}
		}
	}

	private static class StubbedCollection extends GuildCollection {
		int runs;
		RuntimeException failure;

		StubbedCollection() {
			super(null, null, null, () -> {}, null, null, null);
		}

		@Override
		public void run() {
			runs++;
			if (failure != null) {
				throw failure;
			}
		}
	}
}
