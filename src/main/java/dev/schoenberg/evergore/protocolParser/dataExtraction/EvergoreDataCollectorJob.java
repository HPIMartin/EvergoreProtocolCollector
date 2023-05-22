package dev.schoenberg.evergore.protocolParser.dataExtraction;

import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.*;
import static java.util.concurrent.TimeUnit.*;

import dev.schoenberg.evergore.protocolParser.*;
import io.micronaut.scheduling.annotation.*;
import jakarta.inject.*;

@Singleton
public class EvergoreDataCollectorJob {
	public static int DELAY_IN_SEC = 30;

	private final Logger logger;
	private final EvergoreDataExtractor dataExtractor;
	private final EvergoreDataEvaluator evaluation;
	private final PostCollectionHook hook;

	public EvergoreDataCollectorJob(Logger logger, EvergoreDataExtractor dataExtractor, EvergoreDataEvaluator evaluation, PostCollectionHook hook) {
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
		silentThrow(() -> SECONDS.sleep(DELAY_IN_SEC));
	}
}
