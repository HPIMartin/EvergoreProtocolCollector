package dev.schoenberg.evergore.protocolParser.application;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.Executor;

import dev.schoenberg.evergore.protocolParser.Logger;

public class Collections {
	private final GuildCollection collection;
	private final Executor background;
	private final Clock clock;
	private final Logger logger;
	private Instant runningSince;

	public Collections(GuildCollection collection, Executor background, Clock clock, Logger logger) {
		this.collection = collection;
		this.background = background;
		this.clock = clock;
		this.logger = logger;
	}

	public void runDaily() {
		begin();
		logger.info("Daily collection started.");
		try {
			collection.run();
		} finally {
			end();
		}
	}

	public StartOutcome startByHand() {
		synchronized (this) {
			if (runningSince != null) {
				return new StartOutcome.AlreadyRunning(runningSince);
			}
			begin();
		}
		logger.info("Collection started by hand.");
		background.execute(this::runStartedByHand);
		return new StartOutcome.Started();
	}

	public synchronized Optional<Instant> runningSince() {
		return Optional.ofNullable(runningSince);
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

	private synchronized void begin() {
		runningSince = clock.instant();
	}

	private synchronized void end() {
		runningSince = null;
	}
}
