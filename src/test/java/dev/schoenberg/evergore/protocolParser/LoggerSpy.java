package dev.schoenberg.evergore.protocolParser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LoggerSpy implements Logger {
	private final List<String> infoMessages = new ArrayList<>();
	private final List<String> warnMessages = new ArrayList<>();
	private final List<String> errorMessages = new ArrayList<>();

	@Override
	public void info(String toLog) {
		infoMessages.add(toLog);
	}

	@Override
	public void warn(String toLog) {
		warnMessages.add(toLog);
	}

	@Override
	public void error(String reason) {
		errorMessages.add(reason);
	}

	@Override
	public void error(String reason, Throwable error) {
		errorMessages.add(reason);
	}

	@Override
	public void debug(String toLog) {}

	public List<String> infoMessages() {
		return Collections.unmodifiableList(infoMessages);
	}

	public List<String> warnMessages() {
		return Collections.unmodifiableList(warnMessages);
	}

	public List<String> errorMessages() {
		return Collections.unmodifiableList(errorMessages);
	}
}
