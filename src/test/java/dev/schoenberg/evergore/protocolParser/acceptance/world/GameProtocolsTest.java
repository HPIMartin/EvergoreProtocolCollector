package dev.schoenberg.evergore.protocolParser.acceptance.world;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import dev.schoenberg.evergore.protocolParser.dataExtraction.PageContents;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class GameProtocolsTest {
	private final GameProtocols tested = new GameProtocols();

	@Test
	void holdsTheAnswerBackUntilItIsReleased() throws InterruptedException {
		tested.holdTheNextAnswer();
		AtomicReference<PageContents> answer = new AtomicReference<>();
		Thread asking = Thread.ofPlatform().start(() -> answer.set(tested.pageContents()));
		tested.awaitAsked();
		awaitWaitingOrDone(asking);
		PageContents whileHeld = answer.get();

		tested.release();
		asking.join();

		assertThat(whileHeld).isNull();
		assertThat(answer.get()).isNotNull();
	}

	@Test
	void holdsOnlyTheAnswersAskedBeforeTheRelease() throws InterruptedException {
		tested.holdTheNextAnswer();
		Thread asking = Thread.ofPlatform().start(tested::pageContents);
		tested.awaitAsked();
		tested.release();
		asking.join();

		PageContents laterAnswer = tested.pageContents();

		assertThat(laterAnswer).isNotNull();
	}

	@Test
	void failsAfterTheReleaseWhenTheGameCannotBeReached() throws InterruptedException {
		tested.holdTheNextAnswer();
		tested.cutOff();
		AtomicReference<Throwable> failure = new AtomicReference<>();
		Thread asking = Thread.ofPlatform().start(() -> failure.set(catchThrowable(tested::pageContents)));
		tested.awaitAsked();
		awaitWaitingOrDone(asking);
		Throwable whileHeld = failure.get();

		tested.release();
		asking.join();

		assertThat(whileHeld).isNull();
		assertThat(failure.get()).isInstanceOf(IllegalStateException.class);
	}

	@Test
	void refusesToAwaitAnAnswerNobodyHolds() {
		Throwable refused = catchThrowable(tested::awaitAsked);

		assertThat(refused).isInstanceOf(AssertionError.class);
	}

	private static void awaitWaitingOrDone(Thread thread) {
		while (thread.getState() != Thread.State.TIMED_WAITING && thread.getState() != Thread.State.TERMINATED) {
			Thread.onSpinWait();
		}
	}
}
