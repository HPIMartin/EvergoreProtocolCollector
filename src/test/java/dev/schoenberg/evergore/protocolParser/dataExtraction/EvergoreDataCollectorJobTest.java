package dev.schoenberg.evergore.protocolParser.dataExtraction;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.schoenberg.evergore.protocolParser.LoggerSpy;
import dev.schoenberg.evergore.protocolParser.application.CreateAccounts;
import dev.schoenberg.evergore.protocolParser.application.EvaluationResult;
import dev.schoenberg.evergore.protocolParser.application.EvaluationResultFixture;
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
	private RecordingCreateAccounts accounts;
	private AtomicInteger hookRunCount;
	private LoggerSpy logger;
	private EvergoreDataCollectorJob tested;

	@BeforeEach
	void setup() {
		lastRunStatus = new LastRunStatus();
		extractor = new FailableExtractor();
		evaluator = new FailableEvaluator();
		accounts = new RecordingCreateAccounts(evaluator);
		hookRunCount = new AtomicInteger();
		logger = new LoggerSpy();
		tested = new EvergoreDataCollectorJob(new ZeroDelayConfiguration(), extractor, evaluator, accounts, hookRunCount::incrementAndGet, lastRunStatus,
				Clock.fixed(FIXED_NOW, ZoneOffset.UTC), logger);
	}

	@Test
	void createsTheAccountsOfEveryKnownAvatarAfterTheRecompute() {
		tested.scheduleEvery24Hours();

		assertThat(accounts.runsAfterTheRecompute).isEqualTo(1);
		assertThat(accounts.runsBeforeTheRecompute).isZero();
	}

	@Test
	void createsTheAccountsAfterAFailedScrapeToo() {
		extractor.failOnLoad = true;

		tested.scheduleEvery24Hours();

		assertThat(accounts.runsAfterTheRecompute).isEqualTo(1);
	}

	@Test
	void keepsTheRecomputesOutcomeWhenCreatingTheAccountsFails() {
		accounts.failOnCreate = true;

		tested.scheduleEvery24Hours();

		assertThat(lastRunStatus.snapshot().lastSuccessfulRecompute()).contains(FIXED_NOW);
		assertThat(lastRunStatus.snapshot().lastRecomputeFailure()).isEmpty();
	}

	@Test
	void runsTheHookWhenCreatingTheAccountsFails() {
		accounts.failOnCreate = true;

		tested.scheduleEvery24Hours();

		assertThat(hookRunCount.get()).isEqualTo(1);
	}

	@Test
	void logsTheFailureOfCreatingTheAccountsAsAnError() {
		accounts.failOnCreate = true;

		tested.scheduleEvery24Hours();

		assertThat(logger.errorMessages()).containsExactly("Creating the accounts failed; the recompute stands.");
		assertThat(logger.errorThrowables()).extracting(Throwable::getMessage).containsExactly("createAccounts failed");
	}

	@Test
	void createsNoAccountsWhenTheRecomputeFails() {
		evaluator.failOnEvaluate = true;

		assertThatThrownBy(() -> tested.scheduleEvery24Hours()).isInstanceOf(RuntimeException.class);

		assertThat(accounts.runsBeforeTheRecompute + accounts.runsAfterTheRecompute).isZero();
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
		EvergoreDataCollectorJob interruptibleJob = new EvergoreDataCollectorJob(new PositiveDelayConfiguration(), extractor, evaluator, accounts, () -> {}, lastRunStatus,
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

	private static class RecordingCreateAccounts extends CreateAccounts {
		private final FailableEvaluator evaluator;
		boolean failOnCreate;
		int runsBeforeTheRecompute;
		int runsAfterTheRecompute;

		RecordingCreateAccounts(FailableEvaluator evaluator) {
			super(null, null, null, null, null, null);
			this.evaluator = evaluator;
		}

		@Override
		public void forEveryKnownAvatar() {
			if (evaluator.evaluateCalled) {
				runsAfterTheRecompute++;
			} else {
				runsBeforeTheRecompute++;
			}
			if (failOnCreate) {
				throw new RuntimeException("createAccounts failed");
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
			return new EvaluationResultFixture().unknownItemNames(unknownItems).zeroValuedItemNames(zeroValuedItems).build();
		}
	}
}
