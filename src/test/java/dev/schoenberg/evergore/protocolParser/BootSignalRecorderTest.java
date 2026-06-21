package dev.schoenberg.evergore.protocolParser;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BootSignalRecorderTest {
	@Test
	void recordCollectionFinishedFlipsQueryFromFalseToTrue() {
		BootSignalRecorder recorder = new BootSignalRecorder();

		assertThat(recorder.awaitCollection(Duration.ofMillis(1))).isFalse();
		recorder.recordCollectionFinished();

		assertThat(recorder.awaitCollection(Duration.ofMillis(10))).isTrue();
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

	@Test
	void awaitCollectionReturnsTrueWhenCollectionAlreadyFinished() {
		BootSignalRecorder recorder = new BootSignalRecorder();
		recorder.recordCollectionFinished();

		boolean result = recorder.awaitCollection(Duration.ofMillis(100));

		assertThat(result).isTrue();
	}

	@Test
	void awaitCollectionReturnsFalseOnTimeoutWhenCollectionNeverFinishes() {
		BootSignalRecorder recorder = new BootSignalRecorder();

		boolean result = recorder.awaitCollection(Duration.ofMillis(50));

		assertThat(result).isFalse();
	}
}
