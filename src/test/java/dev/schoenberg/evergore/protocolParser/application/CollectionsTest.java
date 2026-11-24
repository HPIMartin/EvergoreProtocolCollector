package dev.schoenberg.evergore.protocolParser.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.schoenberg.evergore.protocolParser.LoggerSpy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class CollectionsTest {
	private StubbedCollection collection;
	private LoggerSpy logger;
	private Collections tested;

	@BeforeEach
	void setup() {
		collection = new StubbedCollection();
		logger = new LoggerSpy();
		tested = new Collections(collection, logger);
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
