package dev.schoenberg.evergore.protocolParser;

import java.time.Duration;
import java.time.Instant;

import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.silentThrow;
import static java.time.Instant.now;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class BootSignalRecorder {
	private volatile boolean collectionFinished;
	private volatile boolean dataLoaded;
	private volatile boolean exceptionOccurred;

	public void recordCollectionFinished() {
		collectionFinished = true;
	}

	public void recordDataLoaded() {
		dataLoaded = true;
	}

	public void recordException() {
		exceptionOccurred = true;
	}

	public boolean awaitCollection(Duration timeout) {
		Instant deadline = now().plus(timeout);
		while (!collectionFinished) {
			if (now().isAfter(deadline)) {
				return false;
			}
			silentThrow(() -> MILLISECONDS.sleep(50));
		}
		return true;
	}

	public boolean dataLoaded() {
		return dataLoaded;
	}

	public boolean exceptionOccurred() {
		return exceptionOccurred;
	}
}
