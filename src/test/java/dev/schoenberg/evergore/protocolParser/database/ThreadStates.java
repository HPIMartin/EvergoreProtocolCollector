package dev.schoenberg.evergore.protocolParser.database;

import java.time.Duration;

import static java.lang.Thread.State.NEW;
import static java.lang.Thread.State.RUNNABLE;

final class ThreadStates {
	static final Duration SETTLING_BOUND = Duration.ofSeconds(10);

	private ThreadStates() {}

	static Thread.State settled(Thread thread) {
		long deadline = System.nanoTime() + SETTLING_BOUND.toNanos();
		Thread.State state = thread.getState();
		while ((state == NEW || state == RUNNABLE) && System.nanoTime() < deadline) {
			Thread.onSpinWait();
			state = thread.getState();
		}
		return state;
	}
}
