package dev.schoenberg.evergore.protocolParser;

import jakarta.inject.Singleton;

import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.context.annotation.Requires;

import dev.schoenberg.evergore.protocolParser.helper.logger.Slf4jLogger;

@Factory
@Requires(property = SpiedLoggerFactory.SPIED_LOGGER)
public class SpiedLoggerFactory {
	public static final String SPIED_LOGGER = "test.spied-logger";

	@Singleton
	@Replaces(Slf4jLogger.class)
	LoggerSpy spiedLogger() {
		return new LoggerSpy();
	}
}
