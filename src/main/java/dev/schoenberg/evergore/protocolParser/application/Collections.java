package dev.schoenberg.evergore.protocolParser.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.Executor;

import dev.schoenberg.evergore.protocolParser.Logger;

public class Collections {
	private static final Duration HOUR_BETWEEN_STARTS_BY_HAND = Duration.ofHours(1);

	private final GuildCollection collection;
	private final Executor background;
	private final Clock clock;
	private final Logger logger;
	private Instant runningSince;
	private Instant lastStart;

	public Collections(GuildCollection collection, Executor background, Clock clock, Logger logger) {
		this.collection = collection;
		this.background = background;
		this.clock = clock;
		this.logger = logger;
	}

	public void runDaily() {
		if (!beginDaily()) {
			logger.info("Daily collection skipped: a collection is already running.");
			return;
		}
		logger.info("Daily collection started.");
		try {
			collection.run();
		} finally {
			end();
		}
	}

	public StartOutcome startByHand() {
		StartOutcome outcome = beginByHand();
		if (outcome instanceof StartOutcome.Started) {
			logger.info("Collection started by hand.");
			background.execute(this::runStartedByHand);
		}
		return outcome;
	}

	public synchronized Optional<Instant> runningSince() {
		return Optional.ofNullable(runningSince);
	}

	public synchronized Optional<Instant> nextStartByHandFrom() {
		if (runningSince != null || lastStart == null) {
			return Optional.empty();
		}
		Instant from = lastStart.plus(HOUR_BETWEEN_STARTS_BY_HAND);
		return clock.instant().isBefore(from) ? Optional.of(from) : Optional.empty();
	}

	private void runStartedByHand() {
		try {
			collection.run();
		} catch (RuntimeException e) {
			logger.error("Collection started by hand failed.", e);
		} finally {
			end();
		}
	}

	private synchronized boolean beginDaily() {
		if (runningSince != null) {
			return false;
		}
		begin(clock.instant());
		return true;
	}

	private synchronized StartOutcome beginByHand() {
		if (runningSince != null) {
			return new StartOutcome.AlreadyRunning(runningSince);
		}
		Optional<Instant> acceptedFrom = nextStartByHandFrom();
		if (acceptedFrom.isPresent()) {
			return new StartOutcome.TooEarly(acceptedFrom.get());
		}
		begin(clock.instant());
		return new StartOutcome.Started();
	}

	private void begin(Instant now) {
		runningSince = now;
		lastStart = now;
	}

	private synchronized void end() {
		runningSince = null;
	}
}
