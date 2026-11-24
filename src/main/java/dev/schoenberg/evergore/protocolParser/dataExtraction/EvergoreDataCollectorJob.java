package dev.schoenberg.evergore.protocolParser.dataExtraction;

import jakarta.inject.Singleton;

import io.micronaut.scheduling.annotation.Scheduled;

import dev.schoenberg.evergore.protocolParser.application.Collections;
import dev.schoenberg.evergore.protocolParser.helper.config.Configuration;

import static java.util.concurrent.TimeUnit.SECONDS;

@Singleton
public class EvergoreDataCollectorJob {
	private final Configuration config;
	private final Collections collections;

	public EvergoreDataCollectorJob(Configuration config, Collections collections) {
		this.config = config;
		this.collections = collections;
	}

	@Scheduled(fixedDelay = "24h")
	void scheduleEvery24Hours() {
		initialDelay();
		collections.runDaily();
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
