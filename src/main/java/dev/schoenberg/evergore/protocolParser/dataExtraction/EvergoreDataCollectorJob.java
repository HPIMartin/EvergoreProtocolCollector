package dev.schoenberg.evergore.protocolParser.dataExtraction;

import static dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey.*;
import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.*;
import static java.util.Arrays.*;
import static java.util.concurrent.TimeUnit.*;

import java.time.*;
import java.util.*;

import dev.schoenberg.evergore.protocolParser.*;
import dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.*;
import io.micronaut.scheduling.annotation.*;
import jakarta.inject.*;

@Singleton
public class EvergoreDataCollectorJob {
	public static int DELAY_IN_SEC = 30;

	private final Logger logger;
	private final EvergoreDataExtractor dataExtractor;
	private final MetaInformationRepository metaRepo;
	private final PostCollectionHook hook;

	public EvergoreDataCollectorJob(Logger logger, EvergoreDataExtractor dataExtractor, MetaInformationRepository metaRepo, PostCollectionHook hook) {
		this.logger = logger;
		this.dataExtractor = dataExtractor;
		this.metaRepo = metaRepo;
		this.hook = hook;
	}

	@Scheduled(fixedDelay = "24h")
	void scheduleEvery24Hours() {
		initialDelay();
		logger.info("Scheduled extraction started...");
		dataExtractor.loadData();
		logger.info("Scheduled extraction finished!");
		logger.info("Evaluate Data...");
		evaluateData();
		logger.info("Data evaluation done!");
		hook.run();
	}

	private void initialDelay() {
		silentThrow(() -> SECONDS.sleep(DELAY_IN_SEC));
	}

	private void evaluateData() {
		logger.info("Old value: " + getLastUpdated());

		LocalDateTime now = LocalDateTime.now();
		MetaInformation<LocalDateTime> newUpdatedInformation = new MetaInformation<>(LAST_UPDATED, now);
		metaRepo.add(asList(newUpdatedInformation));

		logger.info("New value: " + getLastUpdated());
	}

	private String getLastUpdated() {
		Optional<MetaInformation<LocalDateTime>> lastUpdatedInformation = metaRepo.get(LAST_UPDATED);
		return lastUpdatedInformation.map(x -> x.value).map(LocalDateTime::toString).orElse("Not defined");
	}
}
