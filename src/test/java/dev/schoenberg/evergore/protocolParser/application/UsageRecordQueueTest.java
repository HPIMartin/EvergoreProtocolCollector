package dev.schoenberg.evergore.protocolParser.application;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import dev.schoenberg.evergore.protocolParser.LoggerSpy;
import dev.schoenberg.evergore.protocolParser.businessLogic.usage.UsageKind;
import dev.schoenberg.evergore.protocolParser.businessLogic.usage.UsageRecord;

import static org.assertj.core.api.Assertions.assertThat;

class UsageRecordQueueTest {
	private static final Instant LOGGED_IN = Instant.parse("2026-11-01T20:15:00Z");
	private static final UsageWriting WRITING = new UsageWriting(10, 500, Duration.ofSeconds(10));

	private final RecordingUsageRecords repository = new RecordingUsageRecords();
	private final LoggerSpy logger = new LoggerSpy();

	@Test
	void closingWritesEveryRecordRecordedBeforeItInTheOrderTheyCame() {
		UsageRecordQueue tested = UsageRecordQueue.start(repository, WRITING, runnable -> new Thread(runnable, "usage-writer-test"), logger);
		List<UsageRecord> recorded = List.of(login("aurora"), login("boreas"), login("ceres"));

		recorded.forEach(tested::record);
		tested.close();

		assertThat(repository.records()).containsExactlyElementsOf(recorded);
	}

	private static UsageRecord login(String accountKey) {
		return new UsageRecord(accountKey, LOGGED_IN, UsageKind.LOGIN, Optional.empty());
	}
}
