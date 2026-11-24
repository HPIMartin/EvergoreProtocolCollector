package dev.schoenberg.evergore.protocolParser;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadFactory;

import io.micronaut.context.ApplicationContext;
import io.micronaut.inject.qualifiers.Qualifiers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.schoenberg.evergore.protocolParser.application.ThreadEnd;
import dev.schoenberg.evergore.protocolParser.application.UsageRecordQueue;
import dev.schoenberg.evergore.protocolParser.businessLogic.usage.UsageKind;
import dev.schoenberg.evergore.protocolParser.businessLogic.usage.UsageRecord;
import dev.schoenberg.evergore.protocolParser.database.SqliteFile;

import static dev.schoenberg.evergore.protocolParser.ThrowawayDatabaseFactory.THROWAWAY_DATABASE_PATH;
import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.silentThrow;
import static org.assertj.core.api.Assertions.assertThat;

class UsageRecordQueueWiringTest {
	private static final String DATABASE = "build/tmp/test/usageRecordQueueWiring.sqlite";
	private static final Instant LOGGED_IN = Instant.parse("2026-11-01T20:15:00Z");

	private final CountDownLatch closeBegun = new CountDownLatch(1);

	@BeforeEach
	void deleteTheThrowawayDatabase() {
		silentThrow(() -> Files.deleteIfExists(Paths.get(DATABASE)));
	}

	@Test
	void aContextClosingWithRecordsWaitingLeavesThemInTheDatabaseFile() {
		ThreadFactory writerWaitingForTheClose = writing -> new Thread(() -> {
			silentThrow(() -> closeBegun.await());
			writing.run();
		}, "usage-writer-test");
		ThreadEnd releasingTheWriter = (thread, bound) -> {
			closeBegun.countDown();
			thread.join();
			return true;
		};
		try (ApplicationContext context = ApplicationContext.builder("test").properties(Map.of(THROWAWAY_DATABASE_PATH, DATABASE)).build()) {
			context.registerSingleton(ThreadFactory.class, writerWaitingForTheClose, Qualifiers.byName("usage-writer"));
			context.registerSingleton(ThreadEnd.class, releasingTheWriter);
			context.start();
			UsageRecordQueue queue = context.getBean(UsageRecordQueue.class);

			queue.record(new UsageRecord("aurora", LOGGED_IN, UsageKind.LOGIN, Optional.empty()));
			queue.record(new UsageRecord("aurora", LOGGED_IN, UsageKind.PAGE_LOAD, Optional.empty()));
		}

		assertThat(new SqliteFile(DATABASE).committedTexts("usage_record", "account_key || '|' || what")).containsExactly("aurora|LOGIN", "aurora|PAGE_LOAD");
	}
}
