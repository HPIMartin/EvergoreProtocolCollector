package dev.schoenberg.evergore.protocolParser.dataExtraction;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.schoenberg.evergore.protocolParser.LoggerSpy;
import dev.schoenberg.evergore.protocolParser.application.EvaluationResult;
import dev.schoenberg.evergore.protocolParser.application.EvergoreDataEvaluator;
import dev.schoenberg.evergore.protocolParser.application.EvergoreDataExtractor;
import dev.schoenberg.evergore.protocolParser.application.LastRunStatus;
import dev.schoenberg.evergore.protocolParser.businessLogic.banking.BankRepositoryStub;
import dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.FakeMetaInformationRepository;
import dev.schoenberg.evergore.protocolParser.businessLogic.storage.StorageRepositoryStub;
import dev.schoenberg.evergore.protocolParser.helper.config.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EvergoreDataCollectorJobTest {

	private static final Instant FIXED_NOW = Instant.parse("2026-06-21T12:00:00Z");

	private LastRunStatus lastRunStatus;
	private FailableExtractor extractor;
	private FailableEvaluator evaluator;
	private AtomicInteger hookRunCount;
	private EvergoreDataCollectorJob tested;

	@BeforeEach
	void setup() {
		lastRunStatus = new LastRunStatus();
		extractor = new FailableExtractor();
		evaluator = new FailableEvaluator();
		hookRunCount = new AtomicInteger();
		tested = new EvergoreDataCollectorJob(new ZeroDelayConfiguration(), extractor, evaluator, hookRunCount::incrementAndGet, lastRunStatus,
				Clock.fixed(FIXED_NOW, ZoneOffset.UTC), new LoggerSpy());
	}

	@Test
	void recordsSuccessfulScrapeAndRecomputeAfterASuccessfulRun() {
		tested.scheduleEvery24Hours();

		assertThat(lastRunStatus.snapshot().lastSuccessfulScrape()).contains(FIXED_NOW);
		assertThat(lastRunStatus.snapshot().lastSuccessfulRecompute()).contains(FIXED_NOW);
	}

	@Test
	void recomputeStillRunsAfterAFailedScrape() {
		extractor.failOnLoad = true;

		tested.scheduleEvery24Hours();

		assertThat(evaluator.evaluateCalled).isTrue();
		assertThat(lastRunStatus.snapshot().lastScrapeFailure()).contains(FIXED_NOW);
		assertThat(lastRunStatus.snapshot().lastSuccessfulScrape()).isEmpty();
		assertThat(lastRunStatus.snapshot().lastSuccessfulRecompute()).contains(FIXED_NOW);
	}

	@Test
	void doesNotRecordWhenEvaluateDataThrows() {
		evaluator.failOnEvaluate = true;

		assertThatThrownBy(() -> tested.scheduleEvery24Hours()).isInstanceOf(RuntimeException.class);

		assertThat(lastRunStatus.snapshot().lastSuccessfulScrape()).contains(FIXED_NOW);
		assertThat(lastRunStatus.snapshot().lastRecomputeFailure()).contains(FIXED_NOW);
		assertThat(lastRunStatus.snapshot().lastSuccessfulRecompute()).isEmpty();
	}

	@Test
	void doesNotRunTheHookWhenRecomputeFails() {
		evaluator.failOnEvaluate = true;

		assertThatThrownBy(() -> tested.scheduleEvery24Hours()).isInstanceOf(RuntimeException.class);

		assertThat(hookRunCount.get()).isZero();
	}

	@Test
	void runsTheHookAfterAFailedScrapeButASuccessfulRecompute() {
		extractor.failOnLoad = true;

		tested.scheduleEvery24Hours();

		assertThat(hookRunCount.get()).isEqualTo(1);
	}

	@Test
	void restoresInterruptFlagWhenInitialDelayIsInterrupted() {
		EvergoreDataCollectorJob interruptibleJob = new EvergoreDataCollectorJob(new PositiveDelayConfiguration(), extractor, evaluator, () -> {}, lastRunStatus,
				Clock.fixed(FIXED_NOW, ZoneOffset.UTC), new LoggerSpy());
		Thread.currentThread().interrupt();
		try {
			assertThatThrownBy(() -> interruptibleJob.scheduleEvery24Hours()).isInstanceOf(RuntimeException.class);

			assertThat(Thread.currentThread().isInterrupted()).isTrue();
		} finally {
			Thread.interrupted();
		}
	}

	@Test
	void handsBothNameListsOfTheRecomputeToTheLastRunStatusWithoutSwappingThem() {
		evaluator.unknownItems = List.of("Unobtainium");
		evaluator.zeroValuedItems = List.of("Übungsstück-Sorandilaxt");

		tested.scheduleEvery24Hours();

		assertThat(lastRunStatus.snapshot().unknownItemNames()).containsExactly("Unobtainium");
		assertThat(lastRunStatus.snapshot().zeroValuedItemNames()).containsExactly("Übungsstück-Sorandilaxt");
	}

	@Test
	void forwardsTheRunsUnknownItemsToLastRunStatus() {
		evaluator.unknownItems = List.of("Unobtainium");

		tested.scheduleEvery24Hours();

		assertThat(lastRunStatus.snapshot().unknownItemNames()).containsExactly("Unobtainium");
	}

	private static class ZeroDelayConfiguration extends Configuration {
		@Override
		public int getCollectorInitialDelaySeconds() {
			return 0;
		}
	}

	private static class PositiveDelayConfiguration extends Configuration {
		@Override
		public int getCollectorInitialDelaySeconds() {
			return 5;
		}
	}

	private static class FailableExtractor extends EvergoreDataExtractor {
		boolean failOnLoad;

		FailableExtractor() {
			super(null, null, null, null);
		}

		@Override
		public void loadData() {
			if (failOnLoad) {
				throw new RuntimeException("loadData failed");
			}
		}
	}

	private static class FailableEvaluator extends EvergoreDataEvaluator {
		boolean failOnEvaluate;
		boolean evaluateCalled;
		List<String> unknownItems = List.of();
		List<String> zeroValuedItems = List.of();

		FailableEvaluator() {
			super(new FakeMetaInformationRepository(), new StorageRepositoryStub(), new BankRepositoryStub(), null, Clock.fixed(FIXED_NOW, ZoneOffset.UTC), new LoggerSpy());
		}

		@Override
		public EvaluationResult evaluateData() {
			evaluateCalled = true;
			if (failOnEvaluate) {
				throw new RuntimeException("evaluateData failed");
			}
			return new EvaluationResult(unknownItems, zeroValuedItems, List.of());
		}
	}
}
