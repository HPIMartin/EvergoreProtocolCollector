package dev.schoenberg.evergore.protocolParser;

import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.Test;

import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.silentThrow;
import static org.assertj.core.api.Assertions.assertThat;

class BootSignalRecorderTest {
	@Test
	void awaitCollectionUnblocksOnceCollectionFinishes() {
		BootSignalRecorder recorder = new BootSignalRecorder();
		CountDownLatch returned = new CountDownLatch(1);
		Thread waiter = new Thread(() -> {
			recorder.awaitCollection();
			returned.countDown();
		});

		waiter.start();
		recorder.recordCollectionFinished();

		silentThrow(() -> returned.await());
	}

	@Test
	void awaitCollectionUnblocksWhenAnExceptionIsRecorded() {
		BootSignalRecorder recorder = new BootSignalRecorder();
		CountDownLatch returned = new CountDownLatch(1);
		Thread waiter = new Thread(() -> {
			recorder.awaitCollection();
			returned.countDown();
		});

		waiter.start();
		recorder.recordException();

		silentThrow(() -> returned.await());
	}

	@Test
	void recordDataLoadedFlipsQueryFromFalseToTrue() {
		BootSignalRecorder recorder = new BootSignalRecorder();

		assertThat(recorder.dataLoaded()).isFalse();
		recorder.recordDataLoaded();

		assertThat(recorder.dataLoaded()).isTrue();
	}

	@Test
	void recordExceptionFlipsQueryFromFalseToTrue() {
		BootSignalRecorder recorder = new BootSignalRecorder();

		assertThat(recorder.exceptionOccurred()).isFalse();
		recorder.recordException();

		assertThat(recorder.exceptionOccurred()).isTrue();
	}
}
