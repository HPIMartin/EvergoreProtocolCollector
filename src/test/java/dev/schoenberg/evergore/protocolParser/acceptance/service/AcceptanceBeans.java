package dev.schoenberg.evergore.protocolParser.acceptance.service;

import java.time.Clock;
import java.util.concurrent.ExecutorService;

import jakarta.inject.Named;
import jakarta.inject.Singleton;

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.context.annotation.Requires;

import dev.schoenberg.evergore.protocolParser.ApplicationFactory;
import dev.schoenberg.evergore.protocolParser.acceptance.world.GameProtocols;
import dev.schoenberg.evergore.protocolParser.acceptance.world.LedgerFaults;
import dev.schoenberg.evergore.protocolParser.acceptance.world.ScenarioTime;
import dev.schoenberg.evergore.protocolParser.dataExtraction.PageSource;
import dev.schoenberg.evergore.protocolParser.dataExtraction.website.SeleniumPageSource;

@Factory
@Requires(env = AcceptanceEnvironment.NAME)
class AcceptanceBeans {
	@Singleton
	@Replaces(bean = Clock.class, factory = ApplicationFactory.class)
	Clock clock(ScenarioTime time) {
		return time.clock();
	}

	@Singleton
	@Named("collections")
	@Replaces(bean = ExecutorService.class, named = "collections", factory = ApplicationFactory.class)
	@Bean(preDestroy = "shutdownNow")
	TrackedCollections collectionExecutor(LedgerFaults faults) {
		return new TrackedCollections(faults);
	}

	@Singleton
	@Replaces(SeleniumPageSource.class)
	PageSource pageSource(GameProtocols protocols) {
		return protocols::pageContents;
	}
}
