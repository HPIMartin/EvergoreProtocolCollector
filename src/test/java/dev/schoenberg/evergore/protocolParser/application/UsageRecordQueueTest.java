package dev.schoenberg.evergore.protocolParser.application;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.Test;

import dev.schoenberg.evergore.protocolParser.LoggerSpy;
import dev.schoenberg.evergore.protocolParser.businessLogic.usage.UsageKind;
import dev.schoenberg.evergore.protocolParser.businessLogic.usage.UsageRecord;

import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.silentThrow;
import static org.assertj.core.api.Assertions.assertThat;

class UsageRecordQueueTest {
	private static final Instant LOGGED_IN = Instant.parse("2026-11-01T20:15:00Z");
	private static final UsageWriting WRITING = new UsageWriting(10, 500, Duration.ofSeconds(10));
	private static final Duration HANG_GUARD = Duration.ofSeconds(10);

	private final RecordingUsageRecords repository = new RecordingUsageRecords();
	private final LoggerSpy logger = new LoggerSpy();
	private final List<String> awaitedEnds = new CopyOnWriteArrayList<>();
	private final ThreadEnd joining = (thread, bound) -> {
		awaitedEnds.add(thread.getName() + " within " + bound);
		thread.join();
		return true;
	};

	@Test
	void closingAwaitsTheEndOfTheWriterThreadWithinTheCloseBound() {
		UsageRecordQueue tested = startedWith(WRITING);

		tested.close();

		assertThat(awaitedEnds).containsExactly("usage-writer-test within PT10S");
	}

	@Test
	void awaitingTheWrittenRecordsReturnsOnlyOnceEveryRecordRecordedBeforeIsWritten() {
		UsageRecordQueue tested = startedWith(WRITING);
		repository.holdTheFirstBatch();
		tested.record(login("aurora"));
		repository.awaitTheFirstBatchWriting();
		Thread awaiting = Thread.ofPlatform().start(tested::awaitWritten);

		Thread.State whileTheWriteHangs = settledStateOf(awaiting);
		repository.releaseTheFirstBatch();
		silentThrow(() -> awaiting.join());
		tested.close();

		assertThat(whileTheWriteHangs).isEqualTo(Thread.State.WAITING);
		assertThat(repository.records()).containsExactly(login("aurora"));
	}

	@Test
	void awaitingTheWrittenRecordsWaitsWhileTheBatchItWasTakenWithIsWritten() {
		UsageRecordQueue tested = startedWith(WRITING);
		repository.holdTheFirstBatch();
		repository.holdTheSecondBatch();
		tested.record(login("aurora"));
		repository.awaitTheFirstBatchWriting();
		tested.record(login("boreas"));
		Thread awaiting = Thread.ofPlatform().start(tested::awaitWritten);
		settledStateOf(awaiting);
		repository.releaseTheFirstBatch();
		repository.awaitTheSecondBatchWriting();

		Thread.State whileItsBatchIsWritten = settledStateOf(awaiting);
		repository.releaseTheSecondBatch();
		silentThrow(() -> awaiting.join());
		tested.close();

		assertThat(whileItsBatchIsWritten).isEqualTo(Thread.State.WAITING);
		assertThat(repository.batches()).containsExactly(List.of(login("aurora")), List.of(login("boreas")));
	}

	@Test
	void awaitingTheWrittenRecordsReturnsAtOnceAfterTheWriterThreadDied() {
		List<Thread> writers = new CopyOnWriteArrayList<>();
		UsageRecordQueue tested = startedRecordingTheWriterIn(writers);
		writers.getFirst().interrupt();
		silentThrow(() -> writers.getFirst().join());

		Thread awaiting = Thread.ofPlatform().daemon().start(tested::awaitWritten);
		boolean returned = silentThrow(() -> awaiting.join(HANG_GUARD));
		tested.close();

		assertThat(returned).isTrue();
	}

	@Test
	void awaitingTheWrittenRecordsReturnsWhenTheWriterThreadDiesInTheWriteBeforeIt() {
		UsageRecordQueue tested = startedWith(WRITING);
		repository.holdAndBreakTheFirstBatch();
		tested.record(login("aurora"));
		repository.awaitTheFirstBatchWriting();
		Thread awaiting = Thread.ofPlatform().daemon().start(tested::awaitWritten);
		settledStateOf(awaiting);

		repository.releaseTheFirstBatch();
		boolean returned = silentThrow(() -> awaiting.join(HANG_GUARD));
		tested.close();

		assertThat(returned).isTrue();
	}

	@Test
	void awaitingTheWrittenRecordsReturnsWhenTheWriterThreadDiesInTheWriteOfItsOwnBatch() {
		UsageRecordQueue tested = startedWith(WRITING);
		repository.holdTheFirstBatch();
		repository.breakTheSecondBatch();
		tested.record(login("aurora"));
		repository.awaitTheFirstBatchWriting();
		tested.record(login("boreas"));
		Thread awaiting = Thread.ofPlatform().daemon().start(tested::awaitWritten);
		settledStateOf(awaiting);

		repository.releaseTheFirstBatch();
		boolean returned = silentThrow(() -> awaiting.join(HANG_GUARD));
		tested.close();

		assertThat(returned).isTrue();
	}

	@Test
	void closingAfterTheWriterThreadDiedCountsAndWarnsOfTheRecordsLeftWaiting() {
		List<Thread> writers = new CopyOnWriteArrayList<>();
		UsageRecordQueue tested = startedRecordingTheWriterIn(writers);
		writers.getFirst().interrupt();
		silentThrow(() -> writers.getFirst().join());

		tested.record(login("aurora"));
		tested.close();

		assertThat(tested.lostSinceStart()).isEqualTo(1);
		assertThat(logger.warnMessages()).containsExactly("Closed with 1 usage records not written");
	}

	@Test
	void closingWritesEveryRecordRecordedBeforeItInTheOrderTheyCame() {
		UsageRecordQueue tested = startedWith(WRITING);
		List<UsageRecord> recorded = List.of(login("aurora"), login("boreas"), login("ceres"));

		recorded.forEach(tested::record);
		tested.close();

		assertThat(repository.records()).containsExactlyElementsOf(recorded);
	}

	@Test
	void writesTheWaitingRecordsInBatchesUpToTheBatchSizeInTheOrderTheyCame() {
		UsageRecordQueue tested = startedWith(new UsageWriting(10, 2, Duration.ofSeconds(10)));
		repository.holdTheFirstBatch();
		tested.record(login("aurora"));
		repository.awaitTheFirstBatchWriting();

		List.of("boreas", "ceres", "dione", "eos", "fenrir").forEach(accountKey -> tested.record(login(accountKey)));
		repository.releaseTheFirstBatch();
		tested.close();

		assertThat(repository.batches())
				.containsExactly(List.of(login("aurora")), List.of(login("boreas"), login("ceres")), List.of(login("dione"), login("eos")), List.of(login("fenrir")));
	}

	@Test
	void takesRecordsUpToItsCapacityWhileAWriteHangsAndDropsAndCountsTheOneOverIt() {
		UsageRecordQueue tested = startedWith(new UsageWriting(3, 500, Duration.ofSeconds(10)));
		repository.holdTheFirstBatch();
		tested.record(login("aurora"));
		repository.awaitTheFirstBatchWriting();

		List.of("boreas", "ceres", "dione", "eos").forEach(accountKey -> tested.record(login(accountKey)));
		long lostWhileTheWriteHangs = tested.lostSinceStart();
		repository.releaseTheFirstBatch();
		tested.close();

		assertThat(lostWhileTheWriteHangs).isEqualTo(1);
		assertThat(repository.records()).containsExactly(login("aurora"), login("boreas"), login("ceres"), login("dione"));
	}

	@Test
	void logsAndCountsTheRecordsOfABatchWhoseWriteFailsAndWritesTheNext() {
		UsageRecordQueue tested = startedWith(WRITING);
		repository.failTheFirstBatch();
		tested.record(login("aurora"));
		repository.awaitTheFirstBatchWriting();

		tested.record(login("boreas"));
		tested.close();

		assertThat(repository.records()).containsExactly(login("boreas"));
		assertThat(tested.lostSinceStart()).isEqualTo(1);
		assertThat(logger.errorMessages()).containsExactly("Lost 1 usage records: their write failed");
		assertThat(logger.errorThrowables()).containsExactly(RecordingUsageRecords.FAILURE);
	}

	@Test
	void countsARecordRecordedAfterTheCloseAsLostAndWritesNothingOfIt() {
		UsageRecordQueue tested = startedWith(WRITING);
		tested.close();

		tested.record(login("aurora"));

		assertThat(tested.lostSinceStart()).isEqualTo(1);
		assertThat(repository.records()).isEmpty();
	}

	@Test
	void closingWhenTheBoundPassesDuringAHangingWriteLogsAndCountsTheRecordsNotWritten() {
		UsageRecordQueue tested = startedWith(WRITING, (thread, bound) -> false);
		repository.holdTheFirstBatch();
		tested.record(login("aurora"));
		repository.awaitTheFirstBatchWriting();
		tested.record(login("boreas"));
		tested.record(login("ceres"));

		tested.close();
		repository.releaseTheFirstBatch();

		assertThat(logger.warnMessages()).containsExactly("Closed with 3 usage records not written");
		assertThat(tested.lostSinceStart()).isEqualTo(3);
	}

	private UsageRecordQueue startedRecordingTheWriterIn(List<Thread> writers) {
		return UsageRecordQueue.start(repository, WRITING, runnable -> {
			Thread thread = new Thread(runnable, "usage-writer-test");
			writers.add(thread);
			return thread;
		}, joining, logger);
	}

	private UsageRecordQueue startedWith(UsageWriting writing) {
		return startedWith(writing, joining);
	}

	private UsageRecordQueue startedWith(UsageWriting writing, ThreadEnd threadEnd) {
		return UsageRecordQueue.start(repository, writing, runnable -> new Thread(runnable, "usage-writer-test"), threadEnd, logger);
	}

	private static UsageRecord login(String accountKey) {
		return new UsageRecord(accountKey, LOGGED_IN, UsageKind.LOGIN, Optional.empty());
	}

	private static Thread.State settledStateOf(Thread thread) {
		Thread.State state = thread.getState();
		while (state != Thread.State.WAITING && state != Thread.State.TERMINATED) {
			Thread.onSpinWait();
			state = thread.getState();
		}
		return state;
	}
}
