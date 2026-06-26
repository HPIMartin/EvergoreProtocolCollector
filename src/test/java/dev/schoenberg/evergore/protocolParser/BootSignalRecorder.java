package dev.schoenberg.evergore.protocolParser;

import java.util.concurrent.CountDownLatch;

import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.silentThrow;

public class BootSignalRecorder {
	private final CountDownLatch collectionFinished = new CountDownLatch(1);
	private volatile boolean dataLoaded;
	private volatile boolean exceptionOccurred;

	public void recordCollectionFinished() {
		collectionFinished.countDown();
	}

	public void recordDataLoaded() {
		dataLoaded = true;
	}

	public void recordException() {
		exceptionOccurred = true;
		collectionFinished.countDown();
	}

	public void awaitCollection() {
		silentThrow(() -> collectionFinished.await());
	}

	public boolean dataLoaded() {
		return dataLoaded;
	}

	public boolean exceptionOccurred() {
		return exceptionOccurred;
	}
}
