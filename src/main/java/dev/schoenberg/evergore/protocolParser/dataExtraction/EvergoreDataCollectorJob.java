package dev.schoenberg.evergore.protocolParser.dataExtraction;

import jakarta.inject.Singleton;

import io.micronaut.scheduling.annotation.Scheduled;

import dev.schoenberg.evergore.protocolParser.application.GuildCollection;
import dev.schoenberg.evergore.protocolParser.helper.config.Configuration;

import static java.util.concurrent.TimeUnit.SECONDS;

@Singleton
public class EvergoreDataCollectorJob {
	private final Configuration config;
	private final GuildCollection collection;

	public EvergoreDataCollectorJob(Configuration config, GuildCollection collection) {
		this.config = config;
		this.collection = collection;
	}

	@Scheduled(fixedDelay = "24h")
	void scheduleEvery24Hours() {
		initialDelay();
		collection.run();
	}

	private void initialDelay() {
		try {
			SECONDS.sleep(config.getCollectorInitialDelaySeconds());
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException(e);
		}
	}
}
