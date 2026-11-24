package dev.schoenberg.evergore.protocolParser.helper.concurrent;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CollectionThreadsTest {
	private final CollectionThreads tested = new CollectionThreads();

	@Test
	void namesTheThreadOfACollectionAfterIt() {
		Thread thread = tested.newThread(() -> {});

		assertThat(thread.getName()).isEqualTo("collection");
	}

	@Test
	void letsTheServiceStopWhileACollectionRuns() {
		Thread thread = tested.newThread(() -> {});

		assertThat(thread.isDaemon()).isTrue();
	}
}
