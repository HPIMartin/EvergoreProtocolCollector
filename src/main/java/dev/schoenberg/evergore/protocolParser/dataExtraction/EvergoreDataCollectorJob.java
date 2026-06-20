package dev.schoenberg.evergore.protocolParser.dataExtraction;

import jakarta.inject.Singleton;

import io.micronaut.scheduling.annotation.Scheduled;

import dev.schoenberg.evergore.protocolParser.Logger;
import dev.schoenberg.evergore.protocolParser.helper.config.Configuration;

import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.silentThrow;
import static java.util.concurrent.TimeUnit.SECONDS;

@Singleton
public class EvergoreDataCollectorJob {
	private final Configuration config;
	private final Logger logger;
	private final EvergoreDataExtractor dataExtractor;
	private final EvergoreDataEvaluator evaluation;
	private final PostCollectionHook hook;

	public EvergoreDataCollectorJob(Configuration config, Logger logger, EvergoreDataExtractor dataExtractor, EvergoreDataEvaluator evaluation, PostCollectionHook hook) {
		this.config = config;
		this.logger = logger;
		this.dataExtractor = dataExtractor;
		this.evaluation = evaluation;
		this.hook = hook;
	}

	@Scheduled(fixedDelay = "24h")
	void scheduleEvery24Hours() {
		initialDelay();
		logger.info("Scheduled extraction started...");
		dataExtractor.loadData();
		logger.info("Scheduled extraction finished!");
		logger.info("Evaluate Data...");
		evaluation.evaluateData();
		logger.info("Data evaluation done!");
		hook.run();
	}

	private void initialDelay() {
		silentThrow(() -> SECONDS.sleep(config.getCollectorInitialDelaySeconds()));
	}
}
