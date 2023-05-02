package dev.schoenberg.evergore.protocolParser.helper.logger;

import org.slf4j.*;

import dev.schoenberg.evergore.protocolParser.Logger;
import jakarta.inject.*;

@Singleton
public class Slf4jLogger implements Logger {
	@Override
	public void info(String toLog) {
		getLogger().info(toLog);
	}

	@Override
	public void debug(String toLog) {
		getLogger().debug(toLog);
	}

	@Override
	public void error(String reason, Throwable error) {
		getLogger().error(reason, error);
	}

	private org.slf4j.Logger getLogger() {
		try {
			StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
			String className = stackTraceElements[3].getClassName();
			return LoggerFactory.getLogger(className);
		} catch (Throwable t) {
			return LoggerFactory.getLogger(this.getClass());
		}
	}
}
