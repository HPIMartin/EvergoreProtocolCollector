package dev.schoenberg.evergore.protocolParser.application;

import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
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
	void namesTheMomentFromWhichAStartByHandIsAcceptedOnlyWithinTheHourOfTheLastStart() {
		Optional<Instant> beforeAnyStart = tested.nextStartByHandFrom();
		tested.runDaily();
		clock.passMinutes(59);
		Optional<Instant> at59Minutes = tested.nextStartByHandFrom();
		clock.passMinutes(1);

		Optional<Instant> at60Minutes = tested.nextStartByHandFrom();

		assertThat(beforeAnyStart).isEmpty();
		assertThat(at59Minutes).contains(START.plusSeconds(3600));
		assertThat(at60Minutes).isEmpty();
	}

	@Test
	void namesNoMomentWhileACollectionRunsAndOneOnceItHasFinishedWithinTheHour() {
		tested.startByHand();
		clock.passMinutes(30);
		Optional<Instant> whileRunning = tested.nextStartByHandFrom();

		background.runHeldTasks();
		Optional<Instant> afterwards = tested.nextStartByHandFrom();

		assertThat(whileRunning).isEmpty();
		assertThat(afterwards).contains(START.plusSeconds(3600));
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

	@Test
	void refusesAStartByHandThatComesBetweenTheCheckAndTheMarkOfTheDailyCollection() {
		AtomicReference<StartOutcome> askedByHand = new AtomicReference<>();
		Thread button = new Thread(() -> askedByHand.set(tested.startByHand()));
		clock.onNextReading(() -> startAndAwaitBlockedOnOrDone(button, tested));
		collection.whileRunning = () -> joined(button);

		tested.runDaily();

		assertThat(askedByHand.get()).isEqualTo(new StartOutcome.AlreadyRunning(START));
		assertThat(background.held()).isZero();
	}

	@Test
	void skipsTheDailyCollectionThatComesBetweenTheCheckAndTheMarkOfAStartByHand() {
		Thread dailyTick = new Thread(() -> tested.runDaily());
		clock.onNextReading(() -> startAndAwaitBlockedOnOrDone(dailyTick, tested));

		StartOutcome outcome = tested.startByHand();
		joined(dailyTick);

		assertThat(outcome).isEqualTo(new StartOutcome.Started());
		assertThat(collection.runs).isZero();
		assertThat(logger.infoMessages()).contains("Daily collection skipped: a collection is already running.");
	}

	@Test
	void statesTheRunningStartAndNoNextStartWhileACollectionRuns() {
		tested.startByHand();
		clock.passMinutes(5);

		CollectionState state = tested.state();

		assertThat(state).isEqualTo(new CollectionState(Optional.of(START), Optional.empty()));
	}

	@Test
	void statesTheNextStartAndNoRunningStartOnceTheCollectionEndedWithinTheHour() {
		tested.startByHand();
		background.runHeldTasks();
		clock.passMinutes(5);

		CollectionState state = tested.state();

		assertThat(state).isEqualTo(new CollectionState(Optional.empty(), Optional.of(START.plusSeconds(3600))));
	}

	@Test
	void readsTheRunningStartAndTheNextStartAsOneStateWhileAnotherThreadEndsTheCollection() {
		Thread ending = new Thread(background::runHeldTasks);
		Collections endedWhileRead = new Collections(collection, background, clock, logger) {
			@Override
			public Optional<Instant> runningSince() {
				Optional<Instant> read = super.runningSince();
				startAndAwaitBlockedOnOrDone(ending, this);
				return read;
			}
		};
		endedWhileRead.startByHand();

		CollectionState state = endedWhileRead.state();
		joined(ending);

		assertThat(state).isEqualTo(new CollectionState(Optional.of(START), Optional.empty()));
	}

	@Test
	void freesTheCollectionAndRethrowsWhenTheExecutorRefusesAStartByHand() {
		RejectedExecutionException refused = new RejectedExecutionException("shut down");
		Collections refusing = new Collections(collection, task -> {
			throw refused;
		}, clock, logger);

		Throwable thrown = catchThrowable(refusing::startByHand);
		refusing.runDaily();

		assertThat(thrown).isSameAs(refused);
		assertThat(collection.runs).isEqualTo(1);
		assertThat(refusing.runningSince()).isEmpty();
	}

	@Test
	void countsAStartByHandTheExecutorRefusedAsNoStart() {
		AtomicBoolean refuse = new AtomicBoolean(true);
		Collections refusingOnce = new Collections(collection, task -> {
			if (refuse.getAndSet(false)) {
				throw new RejectedExecutionException("shut down");
			}
			background.execute(task);
		}, clock, logger);
		catchThrowable(refusingOnce::startByHand);

		StartOutcome again = refusingOnce.startByHand();

		assertThat(again).isEqualTo(new StartOutcome.Started());
	}

	@Test
	void freesTheCollectionWhenItsExecutorWasShutDown() {
		ExecutorService shutDown = Executors.newSingleThreadExecutor();
		shutDown.shutdown();
		Collections onShutDownExecutor = new Collections(collection, shutDown, clock, logger);

		Throwable thrown = catchThrowable(onShutDownExecutor::startByHand);

		assertThat(thrown).isInstanceOf(RejectedExecutionException.class);
		assertThat(onShutDownExecutor.runningSince()).isEmpty();
	}

	@Test
	void logsTheFailureOfAStartByHandTheExecutorRefusesBeforeItRethrows() {
		RejectedExecutionException refused = new RejectedExecutionException("shut down");
		Collections refusing = new Collections(collection, task -> {
			throw refused;
		}, clock, logger);

		Throwable thrown = catchThrowable(refusing::startByHand);

		assertThat(thrown).isSameAs(refused);
		assertThat(logger.errorMessages()).containsExactly("Collection started by hand failed.");
		assertThat(logger.errorThrowables()).containsExactly(refused);
	}

	@Test
	void freesTheCollectionAndRethrowsWhenTheExecutorThrowsAnErrorAtAStartByHand() {
		OutOfMemoryError exhausted = new OutOfMemoryError("unable to create native thread");
		AtomicBoolean fail = new AtomicBoolean(true);
		Collections failingOnce = new Collections(collection, task -> {
			if (fail.getAndSet(false)) {
				throw exhausted;
			}
			background.execute(task);
		}, clock, logger);
		Throwable thrown = catchThrowable(failingOnce::startByHand);

		StartOutcome again = failingOnce.startByHand();

		assertThat(thrown).isSameAs(exhausted);
		assertThat(again).isEqualTo(new StartOutcome.Started());
	}

	private static void startAndAwaitBlockedOnOrDone(Thread thread, Object lockOwner) {
		thread.start();
		while (!isTerminatedOrHeld(thread)) {
			Thread.onSpinWait();
		}
		boolean doneOrBlockedOnTheOwner = thread.getState() == Thread.State.TERMINATED || isBlockedOn(lockOwner, thread);
		assertThat(doneOrBlockedOnTheOwner).isTrue();
	}

	private static boolean isTerminatedOrHeld(Thread thread) {
		Thread.State state = thread.getState();
		return state == Thread.State.TERMINATED || state == Thread.State.BLOCKED || state == Thread.State.WAITING;
	}

	private static boolean isBlockedOn(Object lockOwner, Thread thread) {
		ThreadInfo info = ManagementFactory.getThreadMXBean().getThreadInfo(thread.threadId());
		if (info == null || info.getThreadState() != Thread.State.BLOCKED) {
			return false;
		}
		LockInfo lock = info.getLockInfo();
		return lock != null && lock.getIdentityHashCode() == System.identityHashCode(lockOwner);
	}

	private static void joined(Thread thread) {
		try {
			thread.join();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Interrupted while a thread of the test ran", e);
		}
	}

	private static class SettableClock extends Clock {
		private Instant now;
		private Runnable onNextReading = () -> {};

		SettableClock(Instant now) {
			this.now = now;
		}

		void onNextReading(Runnable hook) {
			onNextReading = hook;
		}

		void passMinutes(long minutes) {
			now = now.plusSeconds(minutes * 60);
		}

		@Override
		public Instant instant() {
			Runnable hook = onNextReading;
			onNextReading = () -> {};
			hook.run();
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
