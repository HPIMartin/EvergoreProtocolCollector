package dev.schoenberg.evergore.protocolParser.acceptance.service;

import jakarta.inject.Singleton;

import io.micronaut.context.annotation.Requires;
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;

import dev.schoenberg.evergore.protocolParser.Logger;
import dev.schoenberg.evergore.protocolParser.acceptance.world.ServiceLog;

@Singleton
@Requires(env = AcceptanceEnvironment.NAME)
class RecordedLog implements BeanCreatedEventListener<Logger> {
	private final ServiceLog log;

	RecordedLog(ServiceLog log) {
		this.log = log;
	}

	@Override
	public Logger onCreated(BeanCreatedEvent<Logger> event) {
		return new Recording(event.getBean(), log);
	}

	private record Recording(Logger logger, ServiceLog log) implements Logger {
		@Override
		public void info(String toLog) {
			log.write(toLog);
			logger.info(toLog);
		}

		@Override
		public void warn(String toLog) {
			log.write(toLog);
			logger.warn(toLog);
		}

		@Override
		public void error(String reason) {
			log.write(reason);
			logger.error(reason);
		}

		@Override
		public void error(String reason, Throwable error) {
			log.write(reason);
			logger.error(reason, error);
		}

		@Override
		public void debug(String toLog) {
			log.write(toLog);
			logger.debug(toLog);
		}
	}
}
