package dev.schoenberg.evergore.protocolParser.application;

import java.time.Clock;

import dev.schoenberg.evergore.protocolParser.Logger;
import dev.schoenberg.evergore.protocolParser.dataExtraction.PostCollectionHook;

public class GuildCollection {
	private final Logger logger;
	private final EvergoreDataExtractor dataExtractor;
	private final EvergoreDataEvaluator evaluation;
	private final CreateAccounts createAccounts;
	private final PostCollectionHook hook;
	private final LastRunStatus lastRunStatus;
	private final Clock clock;

	public GuildCollection(EvergoreDataExtractor dataExtractor, EvergoreDataEvaluator evaluation, CreateAccounts createAccounts, PostCollectionHook hook,
			LastRunStatus lastRunStatus, Clock clock, Logger logger) {
		this.dataExtractor = dataExtractor;
		this.evaluation = evaluation;
		this.createAccounts = createAccounts;
		this.hook = hook;
		this.lastRunStatus = lastRunStatus;
		this.clock = clock;
		this.logger = logger;
	}

	public void run() {
		scrape();
		recompute();
		createAccounts();
		hook.run();
	}

	private void scrape() {
		logger.info("Extraction started...");
		try {
			dataExtractor.loadData();
			lastRunStatus.recordSuccessfulScrape(clock.instant());
			logger.info("Extraction finished!");
		} catch (RuntimeException e) {
			logger.error("Scrape failed; the recompute still runs on the stored rows.", e);
			lastRunStatus.recordScrapeFailure(clock.instant());
		}
	}

	private void recompute() {
		logger.info("Evaluate Data...");
		try {
			EvaluationResult result = evaluation.evaluateData();
			lastRunStatus.recordSuccessfulRecompute(clock.instant(), result);
			logger.info("Data evaluation done!");
		} catch (RuntimeException e) {
			lastRunStatus.recordRecomputeFailure(clock.instant());
			throw e;
		}
	}

	private void createAccounts() {
		try {
			createAccounts.forEveryKnownAvatar();
		} catch (RuntimeException e) {
			logger.error("Creating the accounts failed; the recompute stands.", e);
		}
	}
}
