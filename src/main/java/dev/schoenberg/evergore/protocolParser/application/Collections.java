package dev.schoenberg.evergore.protocolParser.application;

import java.util.concurrent.Executor;

import dev.schoenberg.evergore.protocolParser.Logger;

public class Collections {
	private final GuildCollection collection;
	private final Executor background;
	private final Logger logger;

	public Collections(GuildCollection collection, Executor background, Logger logger) {
		this.collection = collection;
		this.background = background;
		this.logger = logger;
	}

	public void runDaily() {
		logger.info("Daily collection started.");
		collection.run();
	}

	public void startByHand() {
		logger.info("Collection started by hand.");
		background.execute(collection::run);
	}
}
