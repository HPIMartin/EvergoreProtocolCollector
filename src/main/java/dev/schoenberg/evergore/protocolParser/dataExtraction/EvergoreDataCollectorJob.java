package dev.schoenberg.evergore.protocolParser.dataExtraction;

import java.time.*;

import jakarta.inject.*;

import io.micronaut.scheduling.annotation.*;

import dev.schoenberg.evergore.protocolParser.*;
import dev.schoenberg.evergore.protocolParser.helper.config.*;
import dev.schoenberg.evergore.protocolParser.monitoring.*;

import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.*;
import static java.util.concurrent.TimeUnit.*;

@Singleton
public class EvergoreDataCollectorJob {
	private final Configuration config;
	private final Logger logger;
	private final EvergoreDataExtractor dataExtractor;
	private final EvergoreDataEvaluator evaluation;
	private final PostCollectionHook hook;
	private final LastRunStatus lastRunStatus;
	private final Clock clock;

	public EvergoreDataCollectorJob(Configuration config, Logger logger, EvergoreDataExtractor dataExtractor, EvergoreDataEvaluator evaluation, PostCollectionHook hook,
			LastRunStatus lastRunStatus, Clock clock) {
		this.config = config;
		this.logger = logger;
		this.dataExtractor = dataExtractor;
		this.evaluation = evaluation;
		this.hook = hook;
		this.lastRunStatus = lastRunStatus;
		this.clock = clock;
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
		lastRunStatus.recordSuccessfulRun(clock.instant());
		hook.run();
	}

	private void initialDelay() {
		silentThrow(() -> SECONDS.sleep(config.getCollectorInitialDelaySeconds()));
	}
}
