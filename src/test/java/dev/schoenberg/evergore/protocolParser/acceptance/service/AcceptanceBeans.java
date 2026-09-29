package dev.schoenberg.evergore.protocolParser.acceptance.service;

import java.time.Clock;
import java.time.ZoneId;

import jakarta.inject.Singleton;

import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.context.annotation.Requires;

import dev.schoenberg.evergore.protocolParser.ApplicationFactory;
import dev.schoenberg.evergore.protocolParser.acceptance.world.GameProtocols;
import dev.schoenberg.evergore.protocolParser.acceptance.world.OperatorSettings;
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
	@Replaces(bean = ZoneId.class, factory = ApplicationFactory.class)
	ZoneId effectiveZone(OperatorSettings settings) {
		return settings.zone();
	}

	@Singleton
	@Replaces(SeleniumPageSource.class)
	PageSource pageSource(GameProtocols protocols) {
		return protocols::pageContents;
	}
}
