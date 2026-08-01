package dev.schoenberg.evergore.protocolParser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import jakarta.inject.Inject;

import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.scheduling.DefaultTaskExceptionHandler;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import dev.schoenberg.evergore.protocolParser.application.EvergoreDataExtractor;
import dev.schoenberg.evergore.protocolParser.dataExtraction.PostCollectionHook;
import dev.schoenberg.evergore.protocolParser.database.PreDatabaseConnectionHook;
import dev.schoenberg.evergore.protocolParser.domain.EvergoreItem;
import dev.schoenberg.evergore.protocolParser.helper.config.Configuration;

import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.silentThrow;
import static java.util.Arrays.stream;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * On-demand check for the 1:1 verification against production (testing.md): boots the real context against a <em>copy</em> of the production snapshot and lets the real
 * {@code EvergoreDataEvaluator} recompute the meta sums, so the recompute delta can be inspected on real data before a deploy. The scraper is stubbed, so no network, no login and
 * no browser are involved.
 * <p>
 * Disabled by default because it needs a local {@code temp.sqlite}, which is gitignored and holds guild members' data: without it the check has nothing to run against, and it must
 * never become a build gate that only passes on one machine. To run it, drop the {@code @Disabled} for the run and restore it afterwards. It writes nothing outside {@code build/},
 * and never touches the snapshot itself.
 */
@Disabled("On-demand: needs a local production snapshot at temp.sqlite (gitignored)")
@MicronautTest
class ProductionSnapshotRecomputeCheck {
	private static final Path SNAPSHOT = Paths.get("temp.sqlite");
	private static final Path WORKING_DB = Paths.get("build/tmp/prodSnapshot/temp.sqlite");

	static {
		silentThrow(() -> {
			if (!Files.exists(SNAPSHOT)) {
				return;
			}
			Files.createDirectories(WORKING_DB.getParent());
			Files.deleteIfExists(WORKING_DB);
			Files.copy(SNAPSHOT, WORKING_DB);
		});
	}

	private @Inject EmbeddedServer server;
	private @Inject BootSignalRecorder signals;

	@BeforeEach
	void setup() {
		Unirest.config().verifySsl(false);
		Unirest.config().defaultBaseUrl("http://localhost:" + server.getPort());
		signals.awaitCollection();
	}

	@Test
	void recomputesTheProductionSnapshotAndKeepsServingTheOverview() {
		HttpResponse<String> response = Unirest.get("/overview?token=test-token").asString();

		assertThat(response.getStatus()).isBetween(200, 299);
		RenderedTable table = RenderedTable.parse(response.getBody());
		assertThat(table.header()).containsExactly("Avatar", "Entnommen", "Eingelagert");
		assertThat(table.rows()).isNotEmpty();

		write("overview-after-recompute.html", response.getBody());
	}

	/**
	 * Exports the valuation catalog so the recomputed storage sums can be re-derived outside the application (the cross-check that proves a recompute delta is the fix landing, not
	 * a regression). The assertion guards the lookup in {@code EvergoreDataEvaluator.findItem}, which takes the first name match: a duplicate in-game name would make a valuation
	 * ambiguous.
	 */
	@Test
	void exportsTheValuationCatalogAndKeepsItemNamesUnique() {
		StringBuilder catalog = new StringBuilder();
		for (EvergoreItem item : EvergoreItem.values()) {
			catalog.append(item.ingameName).append('\t').append(item.getStorageValue()).append('\t').append(item.getWithdrawlValue()).append('\n');
		}
		write("itemCatalog.tsv", catalog.toString());

		List<String> ingameNames = stream(EvergoreItem.values()).map(item -> item.ingameName).toList();

		assertThat(ingameNames).doesNotHaveDuplicates();
	}

	private void write(String fileName, String content) {
		silentThrow(() -> {
			Files.createDirectories(WORKING_DB.getParent());
			Files.writeString(WORKING_DB.getParent().resolve(fileName), content);
		});
	}

	@MockBean(Configuration.class)
	Configuration configurationMock() {
		return new SnapshotConfiguration();
	}

	public static class SnapshotConfiguration extends Configuration {
		@Override
		public String getDatabasePath() {
			return WORKING_DB.toString();
		}

		@Override
		public int getCollectorInitialDelaySeconds() {
			return 0;
		}
	}

	@MockBean(EvergoreDataExtractor.class)
	StubbedExtractor stubbedExtractor() {
		return new StubbedExtractor();
	}

	public static class StubbedExtractor extends EvergoreDataExtractor {
		public StubbedExtractor() {
			super(null, null, null, null);
		}

		@Override
		public void loadData() {}
	}

	@MockBean(PreDatabaseConnectionHook.class)
	PreDatabaseConnectionHook databaseHook() {
		return new NoOpPreDatabaseConnectionHook();
	}

	public static class NoOpPreDatabaseConnectionHook implements PreDatabaseConnectionHook {
		@Override
		public void run() {}
	}

	@MockBean(PostCollectionHook.class)
	PostCollectionHook collectionHook(BootSignalRecorder recorder) {
		return new CollectionFinishedHook(recorder);
	}

	public static class CollectionFinishedHook implements PostCollectionHook {
		private final BootSignalRecorder signals;

		public CollectionFinishedHook(BootSignalRecorder signals) {
			this.signals = signals;
		}

		@Override
		public void run() {
			signals.recordCollectionFinished();
		}
	}

	@MockBean(DefaultTaskExceptionHandler.class)
	DefaultTaskExceptionHandler exceptionHandler(BootSignalRecorder recorder) {
		return new TestTaskExceptionHandler(recorder);
	}

	public static class TestTaskExceptionHandler extends DefaultTaskExceptionHandler {
		private final BootSignalRecorder signals;

		public TestTaskExceptionHandler(BootSignalRecorder signals) {
			this.signals = signals;
		}

		@Override
		public void handle(Object bean, Throwable throwable) {
			signals.recordException();
		}
	}
}
