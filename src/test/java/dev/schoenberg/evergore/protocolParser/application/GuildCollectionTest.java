package dev.schoenberg.evergore.protocolParser.application;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.schoenberg.evergore.protocolParser.LoggerSpy;
import dev.schoenberg.evergore.protocolParser.businessLogic.banking.BankRepositoryStub;
import dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.FakeMetaInformationRepository;
import dev.schoenberg.evergore.protocolParser.businessLogic.storage.StorageRepositoryStub;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GuildCollectionTest {

	private static final Instant FIXED_NOW = Instant.parse("2026-06-21T12:00:00Z");

	private LastRunStatus lastRunStatus;
	private FailableExtractor extractor;
	private FailableEvaluator evaluator;
	private RecordingCreateAccounts accounts;
	private AtomicInteger hookRunCount;
	private LoggerSpy logger;
	private GuildCollection tested;

	@BeforeEach
	void setup() {
		lastRunStatus = new LastRunStatus();
		extractor = new FailableExtractor();
		evaluator = new FailableEvaluator();
		accounts = new RecordingCreateAccounts(evaluator);
		hookRunCount = new AtomicInteger();
		logger = new LoggerSpy();
		tested = new GuildCollection(extractor, evaluator, accounts, hookRunCount::incrementAndGet, lastRunStatus, Clock.fixed(FIXED_NOW, ZoneOffset.UTC), logger);
	}

	@Test
	void createsTheAccountsOfEveryKnownAvatarAfterTheRecompute() {
		tested.run();

		assertThat(accounts.runsAfterTheRecompute).isEqualTo(1);
		assertThat(accounts.runsBeforeTheRecompute).isZero();
	}

	@Test
	void createsTheAccountsAfterAFailedScrapeToo() {
		extractor.failOnLoad = true;

		tested.run();

		assertThat(accounts.runsAfterTheRecompute).isEqualTo(1);
	}

	@Test
	void keepsTheRecomputesOutcomeWhenCreatingTheAccountsFails() {
		accounts.failOnCreate = true;

		tested.run();

		assertThat(lastRunStatus.snapshot().lastSuccessfulRecompute()).contains(FIXED_NOW);
		assertThat(lastRunStatus.snapshot().lastRecomputeFailure()).isEmpty();
	}

	@Test
	void runsTheHookWhenCreatingTheAccountsFails() {
		accounts.failOnCreate = true;

		tested.run();

		assertThat(hookRunCount.get()).isEqualTo(1);
	}

	@Test
	void logsTheFailureOfCreatingTheAccountsAsAnError() {
		accounts.failOnCreate = true;

		tested.run();

		assertThat(logger.errorMessages()).containsExactly("Creating the accounts failed; the recompute stands.");
		assertThat(logger.errorThrowables()).extracting(Throwable::getMessage).containsExactly("createAccounts failed");
	}

	@Test
	void createsNoAccountsWhenTheRecomputeFails() {
		evaluator.failOnEvaluate = true;

		assertThatThrownBy(() -> tested.run()).isInstanceOf(RuntimeException.class);

		assertThat(accounts.runsBeforeTheRecompute + accounts.runsAfterTheRecompute).isZero();
	}

	@Test
	void recordsSuccessfulScrapeAndRecomputeAfterASuccessfulRun() {
		tested.run();

		assertThat(lastRunStatus.snapshot().lastSuccessfulScrape()).contains(FIXED_NOW);
		assertThat(lastRunStatus.snapshot().lastSuccessfulRecompute()).contains(FIXED_NOW);
	}

	@Test
	void recomputeStillRunsAfterAFailedScrape() {
		extractor.failOnLoad = true;

		tested.run();

		assertThat(evaluator.evaluateCalled).isTrue();
		assertThat(lastRunStatus.snapshot().lastScrapeFailure()).contains(FIXED_NOW);
		assertThat(lastRunStatus.snapshot().lastSuccessfulScrape()).isEmpty();
		assertThat(lastRunStatus.snapshot().lastSuccessfulRecompute()).contains(FIXED_NOW);
	}

	@Test
	void doesNotRecordWhenEvaluateDataThrows() {
		evaluator.failOnEvaluate = true;

		assertThatThrownBy(() -> tested.run()).isInstanceOf(RuntimeException.class);

		assertThat(lastRunStatus.snapshot().lastSuccessfulScrape()).contains(FIXED_NOW);
		assertThat(lastRunStatus.snapshot().lastRecomputeFailure()).contains(FIXED_NOW);
		assertThat(lastRunStatus.snapshot().lastSuccessfulRecompute()).isEmpty();
	}

	@Test
	void doesNotRunTheHookWhenRecomputeFails() {
		evaluator.failOnEvaluate = true;

		assertThatThrownBy(() -> tested.run()).isInstanceOf(RuntimeException.class);

		assertThat(hookRunCount.get()).isZero();
	}

	@Test
	void runsTheHookAfterAFailedScrapeButASuccessfulRecompute() {
		extractor.failOnLoad = true;

		tested.run();

		assertThat(hookRunCount.get()).isEqualTo(1);
	}

	@Test
	void handsBothNameListsOfTheRecomputeToTheLastRunStatusWithoutSwappingThem() {
		evaluator.unknownItems = List.of("Unobtainium");
		evaluator.zeroValuedItems = List.of("Übungsstück-Sorandilaxt");

		tested.run();

		assertThat(lastRunStatus.snapshot().unknownItemNames()).containsExactly("Unobtainium");
		assertThat(lastRunStatus.snapshot().zeroValuedItemNames()).containsExactly("Übungsstück-Sorandilaxt");
	}

	@Test
	void forwardsTheRunsUnknownItemsToLastRunStatus() {
		evaluator.unknownItems = List.of("Unobtainium");

		tested.run();

		assertThat(lastRunStatus.snapshot().unknownItemNames()).containsExactly("Unobtainium");
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
			super(null, null, null, null, null);
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
