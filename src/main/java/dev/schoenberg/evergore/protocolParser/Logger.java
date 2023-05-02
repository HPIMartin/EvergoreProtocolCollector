package dev.schoenberg.evergore.protocolParser;

public interface Logger {
	void info(String toLog);

	void error(String reason, Throwable error);

	void debug(String toLog);
}
