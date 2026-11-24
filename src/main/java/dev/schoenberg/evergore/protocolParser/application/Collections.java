package dev.schoenberg.evergore.protocolParser.application;

import dev.schoenberg.evergore.protocolParser.Logger;

public class Collections {
	private final GuildCollection collection;
	private final Logger logger;

	public Collections(GuildCollection collection, Logger logger) {
		this.collection = collection;
		this.logger = logger;
	}

	public void runDaily() {
		logger.info("Daily collection started.");
		collection.run();
	}
}
