package dev.schoenberg.evergore.protocolParser;

import java.util.*;

public class LoggerSpy implements Logger {
	private final List<String> infoMessages = new ArrayList<>();

	@Override
	public void info(String toLog) {
		infoMessages.add(toLog);
	}

	@Override
	public void error(String reason, Throwable error) {}

	@Override
	public void debug(String toLog) {}

	public List<String> infoMessages() {
		return Collections.unmodifiableList(infoMessages);
	}
}
