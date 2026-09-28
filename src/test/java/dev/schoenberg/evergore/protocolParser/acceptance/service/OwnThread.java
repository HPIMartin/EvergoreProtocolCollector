package dev.schoenberg.evergore.protocolParser.acceptance.service;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

final class OwnThread {
	private OwnThread() {}

	static <T> T call(Supplier<T> work) {
		AtomicReference<T> result = new AtomicReference<>();
		AtomicReference<Throwable> failure = new AtomicReference<>();
		Thread thread = Thread.ofPlatform().start(() -> {
			try {
				result.set(work.get());
			} catch (Throwable e) {
				failure.set(e);
			}
		});
		joinUninterruptibly(thread);
		if (failure.get() instanceof RuntimeException e) {
			throw e;
		}
		if (failure.get() instanceof Error e) {
			throw e;
		}
		return result.get();
	}

	static void run(Runnable work) {
		call(() -> {
			work.run();
			return null;
		});
	}

	private static void joinUninterruptibly(Thread thread) {
		try {
			thread.join();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Interrupted while the service worked on its own thread", e);
		}
	}
}
