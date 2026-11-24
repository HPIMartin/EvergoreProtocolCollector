package dev.schoenberg.evergore.protocolParser.application;

import java.time.Duration;

@FunctionalInterface
public interface ThreadEnd {
	boolean awaited(Thread thread, Duration bound) throws InterruptedException;
}
