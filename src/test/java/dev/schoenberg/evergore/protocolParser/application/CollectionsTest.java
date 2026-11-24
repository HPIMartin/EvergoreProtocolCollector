package dev.schoenberg.evergore.protocolParser.application;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.schoenberg.evergore.protocolParser.LoggerSpy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class CollectionsTest {
	private static final Instant START = Instant.parse("2026-01-02T12:00:00Z");

	private SettableClock clock;
	private StubbedCollection collection;
	private HeldExecutor background;
	private LoggerSpy logger;
	private Collections tested;

	@BeforeEach
	void setup() {
		clock = new SettableClock(START);
		collection = new StubbedCollection();
		background = new HeldExecutor();
		logger = new LoggerSpy();
		tested = new Collections(collection, background, clock, logger);
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

	@Test
	void acceptsAStartByHandWhenNoCollectionRuns() {
		StartOutcome outcome = tested.startByHand();

		assertThat(outcome).isEqualTo(new StartOutcome.Started());
	}

	@Test
	void refusesAStartByHandWhileTheDailyCollectionRuns() {
		AtomicReference<StartOutcome> askedWhileRunning = new AtomicReference<>();
		collection.whileRunning = () -> askedWhileRunning.set(tested.startByHand());

		tested.runDaily();

		assertThat(askedWhileRunning.get()).isEqualTo(new StartOutcome.AlreadyRunning(START));
		assertThat(background.held()).isZero();
	}

	@Test
	void refusesAStartByHandWhileACollectionStartedByHandRuns() {
		tested.startByHand();
		clock.passMinutes(5);

		StartOutcome second = tested.startByHand();
		background.runHeldTasks();

		assertThat(second).isEqualTo(new StartOutcome.AlreadyRunning(START));
		assertThat(collection.runs).isEqualTo(1);
		assertThat(logger.infoMessages()).containsExactly("Collection started by hand.");
	}

	@Test
	void skipsTheDailyCollectionWhileACollectionStartedByHandRuns() {
		tested.startByHand();
		clock.passMinutes(30);

		tested.runDaily();
		background.runHeldTasks();

		assertThat(collection.runs).isEqualTo(1);
		assertThat(logger.infoMessages()).containsExactly("Collection started by hand.", "Daily collection skipped: a collection is already running.");
	}

	@Test
	void doesNotMakeUpASkippedDailyCollectionButRunsTheNextOne() {
		tested.startByHand();
		tested.runDaily();
		background.runHeldTasks();
		clock.passMinutes(24 * 60);

		tested.runDaily();

		assertThat(collection.runs).isEqualTo(2);
		assertThat(logger.infoMessages()).containsExactly("Collection started by hand.", "Daily collection skipped: a collection is already running.", "Daily collection started.");
	}

	@Test
	void keepsTheStartOfTheRunningCollectionWhenTheDailyCollectionIsSkipped() {
		tested.startByHand();
		clock.passMinutes(30);

		tested.runDaily();
		Optional<Instant> stillRunningSince = tested.runningSince();

		assertThat(stillRunningSince).contains(START);
	}

	@Test
	void refusesAStartByHand59MinutesAndAcceptsOneAt60MinutesAfterTheStartOfTheLastCollectionStartedByHand() {
		tested.startByHand();
		background.runHeldTasks();
		clock.passMinutes(59);
		StartOutcome at59Minutes = tested.startByHand();
		clock.passMinutes(1);

		StartOutcome at60Minutes = tested.startByHand();

		assertThat(at59Minutes).isEqualTo(new StartOutcome.TooEarly(START.plusSeconds(3600)));
		assertThat(at60Minutes).isEqualTo(new StartOutcome.Started());
	}

	@Test
	void refusesAStartByHand59MinutesAndAcceptsOneAt60MinutesAfterTheStartOfTheDailyCollection() {
		tested.runDaily();
		clock.passMinutes(59);
		StartOutcome at59Minutes = tested.startByHand();
		clock.passMinutes(1);

		StartOutcome at60Minutes = tested.startByHand();

		assertThat(at59Minutes).isEqualTo(new StartOutcome.TooEarly(START.plusSeconds(3600)));
		assertThat(at60Minutes).isEqualTo(new StartOutcome.Started());
	}

	@Test
	void countsTheHourFromTheStartOfACollectionThatFailed() {
		collection.failure = new IllegalStateException("recompute failed");
		tested.startByHand();
		background.runHeldTasks();
		clock.passMinutes(30);

		StartOutcome outcome = tested.startByHand();

		assertThat(outcome).isEqualTo(new StartOutcome.TooEarly(START.plusSeconds(3600)));
	}

	@Test
	void countsARefusedStartByHandAsNoStart() {
		tested.startByHand();
		background.runHeldTasks();
		clock.passMinutes(30);
		tested.startByHand();
		clock.passMinutes(30);

		StartOutcome outcome = tested.startByHand();

		assertThat(outcome).isEqualTo(new StartOutcome.Started());
	}

	@Test
	void countsASkippedDailyCollectionAsNoStart() {
		tested.startByHand();
		clock.passMinutes(30);
		tested.runDaily();
		background.runHeldTasks();
		clock.passMinutes(29);
		StartOutcome at59Minutes = tested.startByHand();
		clock.passMinutes(1);

		StartOutcome at60Minutes = tested.startByHand();

		assertThat(at59Minutes).isEqualTo(new StartOutcome.TooEarly(START.plusSeconds(3600)));
		assertThat(at60Minutes).isEqualTo(new StartOutcome.Started());
	}

	@Test
	void reportsTheStartOfTheCollectionStartedByHandOnlyWhileItRuns() {
		tested.startByHand();
		clock.passMinutes(5);
		Optional<Instant> whileRunning = tested.runningSince();

		background.runHeldTasks();
		Optional<Instant> afterwards = tested.runningSince();

		assertThat(whileRunning).contains(START);
		assertThat(afterwards).isEmpty();
	}

	@Test
	void reportsTheStartOfTheDailyCollectionOnlyWhileItRuns() {
		AtomicReference<Optional<Instant>> whileRunning = new AtomicReference<>();
		collection.whileRunning = () -> whileRunning.set(tested.runningSince());

		tested.runDaily();
		Optional<Instant> afterwards = tested.runningSince();

		assertThat(whileRunning.get()).contains(START);
		assertThat(afterwards).isEmpty();
	}

	@Test
	void endsAFailedCollectionStartedByHandAndLogsItsError() {
		collection.failure = new IllegalStateException("recompute failed");
		tested.startByHand();

		background.runHeldTasks();

		assertThat(tested.runningSince()).isEmpty();
		assertThat(logger.errorMessages()).containsExactly("Collection started by hand failed.");
		assertThat(logger.errorThrowables()).containsExactly(collection.failure);
	}

	@Test
	void acceptsAStartByHandAfterTheDailyCollectionFailed() {
		collection.failure = new IllegalStateException("recompute failed");
		catchThrowable(() -> tested.runDaily());
		clock.passMinutes(60);

		StartOutcome outcome = tested.startByHand();

		assertThat(outcome).isEqualTo(new StartOutcome.Started());
	}

	private static class SettableClock extends Clock {
		private Instant now;

		SettableClock(Instant now) {
			this.now = now;
		}

		void passMinutes(long minutes) {
			now = now.plusSeconds(minutes * 60);
		}

		@Override
		public Instant instant() {
			return now;
		}

		@Override
		public ZoneId getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(ZoneId zone) {
			throw new UnsupportedOperationException();
		}
	}

	private static class HeldExecutor implements Executor {
		private final Deque<Runnable> held = new ArrayDeque<>();

		@Override
		public void execute(Runnable task) {
			held.add(task);
		}

		int held() {
			return held.size();
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
		Runnable whileRunning = () -> {};

		StubbedCollection() {
			super(null, null, null, () -> {}, null, null, null);
		}

		@Override
		public void run() {
			runs++;
			whileRunning.run();
			if (failure != null) {
				throw failure;
			}
		}
	}
}
