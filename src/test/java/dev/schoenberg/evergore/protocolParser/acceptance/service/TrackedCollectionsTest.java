package dev.schoenberg.evergore.protocolParser.acceptance.service;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import dev.schoenberg.evergore.protocolParser.acceptance.world.LedgerFaults;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class TrackedCollectionsTest {
	private final LedgerFaults faults = new LedgerFaults();
	private final TrackedCollections tested = new TrackedCollections(faults);

	@AfterEach
	void stop() {
		tested.shutdownNow();
	}

	@Test
	void waitsForTheCollectionsItWasGivenToFinish() throws InterruptedException {
		CountDownLatch started = new CountDownLatch(1);
		CountDownLatch go = new CountDownLatch(1);
		AtomicBoolean finished = new AtomicBoolean();
		tested.execute(() -> {
			started.countDown();
			awaitUninterruptibly(go);
			finished.set(true);
		});
		started.await();
		Thread awaiting = Thread.ofPlatform().start(tested::awaitFinished);
		awaitWaitingOrDone(awaiting);
		boolean returnedWhileHeld = awaiting.getState() == Thread.State.TERMINATED;
		boolean finishedWhileHeld = finished.get();

		go.countDown();
		awaiting.join();
		tested.awaitFinished();

		assertThat(returnedWhileHeld).isFalse();
		assertThat(finishedWhileHeld).isFalse();
		assertThat(finished.get()).isTrue();
	}

	@Test
	void runsACollectionWhereSavingTheFiguresCanBeRefused() {
		faults.refuseToSaveFigures();
		Figures figures = faults.guardFigures(Figures.class, () -> "saved");
		AtomicReference<Throwable> failure = new AtomicReference<>();
		tested.execute(() -> failure.set(catchThrowable(figures::save)));

		tested.awaitFinished();

		assertThat(failure.get()).isInstanceOf(IllegalStateException.class);
	}

	private interface Figures {
		String save();
	}

	private static void awaitWaitingOrDone(Thread thread) {
		while (thread.getState() != Thread.State.WAITING && thread.getState() != Thread.State.TERMINATED) {
			Thread.onSpinWait();
		}
	}

	private static void awaitUninterruptibly(CountDownLatch latch) {
		try {
			latch.await();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
}
