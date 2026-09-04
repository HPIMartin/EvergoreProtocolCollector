package dev.schoenberg.evergore.protocolParser.dataExtraction;

import java.time.*;

import jakarta.inject.*;

import io.micronaut.scheduling.annotation.*;

import dev.schoenberg.evergore.protocolParser.*;
import dev.schoenberg.evergore.protocolParser.application.*;
import dev.schoenberg.evergore.protocolParser.helper.config.*;

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

	public EvergoreDataCollectorJob(Configuration config, EvergoreDataExtractor dataExtractor, EvergoreDataEvaluator evaluation, PostCollectionHook hook,
			LastRunStatus lastRunStatus, Clock clock, Logger logger) {
		this.config = config;
		this.dataExtractor = dataExtractor;
		this.evaluation = evaluation;
		this.hook = hook;
		this.lastRunStatus = lastRunStatus;
		this.clock = clock;
		this.logger = logger;
	}

	@Scheduled(fixedDelay = "24h")
	void scheduleEvery24Hours() {
		initialDelay();
		scrape();
		recompute();
		hook.run();
	}

	private void scrape() {
		logger.info("Scheduled extraction started...");
		try {
			dataExtractor.loadData();
			lastRunStatus.recordSuccessfulScrape(clock.instant());
			logger.info("Scheduled extraction finished!");
		} catch (RuntimeException e) {
			logger.error("Scrape failed; the recompute still runs on the stored rows.", e);
			lastRunStatus.recordScrapeFailure(clock.instant());
		}
	}

	private void recompute() {
		logger.info("Evaluate Data...");
		try {
			EvaluationResult result = evaluation.evaluateData();
			lastRunStatus.recordUnknownItems(result.unknownItemNames());
			lastRunStatus.recordFailedAvatars(result.failedAvatarNames());
			lastRunStatus.recordSuccessfulRecompute(clock.instant());
			logger.info("Data evaluation done!");
		} catch (RuntimeException e) {
			lastRunStatus.recordRecomputeFailure(clock.instant());
			throw e;
		}
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
