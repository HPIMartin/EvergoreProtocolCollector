package dev.schoenberg.evergore.protocolParser.acceptance.world;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import dev.schoenberg.evergore.protocolParser.dataExtraction.PageContents;

public class GameProtocols {
	private static final Duration HANG_GUARD = Duration.ofSeconds(120);

	private final GameProtocol bank = new GameProtocol();
	private final GameProtocol storage = new GameProtocol();
	private volatile boolean reachable = true;
	private volatile HeldAnswer held;

	public GameProtocol bank() {
		return bank;
	}

	public GameProtocol storage() {
		return storage;
	}

	public void showNoEntry() {
		bank.show(List.of());
		storage.show(List.of());
	}

	public void cutOff() {
		reachable = false;
	}

	public void reconnect() {
		reachable = true;
	}

	public void holdTheNextAnswer() {
		held = new HeldAnswer();
	}

	public void awaitAsked() {
		HeldAnswer holding = held;
		if (holding == null) {
			throw new AssertionError("The game holds no answer back, so nobody can be waiting for it");
		}
		holding.awaitAsked();
	}

	public void release() {
		HeldAnswer releasing = held;
		held = null;
		if (releasing != null) {
			releasing.release();
		}
	}

	public boolean reachable() {
		return reachable;
	}

	public PageContents pageContents() {
		HeldAnswer holding = held;
		if (holding != null) {
			holding.hold();
		}
		if (!reachable) {
			throw new IllegalStateException("The acceptance scenario made the game unreachable");
		}
		return new PageContents(storage.page(), bank.page());
	}

	public Optional<LocalDateTime> newestMinute() {
		return Stream.of(bank.newestMinute(), storage.newestMinute()).flatMap(Optional::stream).max(LocalDateTime::compareTo);
	}

	private static final class HeldAnswer {
		private final CountDownLatch asked = new CountDownLatch(1);
		private final CountDownLatch released = new CountDownLatch(1);

		void hold() {
			asked.countDown();
			await(released, "released");
		}

		void awaitAsked() {
			await(asked, "asked");
		}

		void release() {
			released.countDown();
		}

		private static void await(CountDownLatch latch, String what) {
			try {
				if (!latch.await(HANG_GUARD.toSeconds(), TimeUnit.SECONDS)) {
					throw new AssertionError("The answer the game holds back was never " + what);
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException("Interrupted while the game held an answer back", e);
			}
		}
	}
}
