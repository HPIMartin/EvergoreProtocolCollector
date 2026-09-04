package dev.schoenberg.evergore.protocolParser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.scheduling.DefaultTaskExceptionHandler;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import dev.schoenberg.evergore.protocolParser.application.EvergoreDataExtractor;
import dev.schoenberg.evergore.protocolParser.dataExtraction.PostCollectionHook;
import dev.schoenberg.evergore.protocolParser.database.PreDatabaseConnectionHook;
import dev.schoenberg.evergore.protocolParser.domain.EvergoreItem;
import dev.schoenberg.evergore.protocolParser.helper.config.Configuration;

import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.silentThrow;
import static dev.schoenberg.evergore.protocolParser.rest.controller.api.PageRequest.MAX_SIZE;
import static io.micronaut.http.HttpStatus.OK;
import static java.util.Arrays.stream;
import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfSystemProperty(named = "prodSnapshot.check", matches = "true", disabledReason = "on-demand: run with -DprodSnapshot.check=true and a local production snapshot")
@MicronautTest
class ProductionSnapshotRecomputeCheck {
	private static final Path SNAPSHOT = Paths.get(System.getProperty("prodSnapshot.file", "temp.sqlite"));
	private static final Path WORKING_DB = Paths.get("build/tmp/prodSnapshot/temp.sqlite");

	private static StoredMetaSums sumsBeforeRecompute = new StoredMetaSums(Map.of());

	static {
		silentThrow(() -> {
			if (!Files.exists(SNAPSHOT)) {
				return;
			}
			Files.createDirectories(WORKING_DB.getParent());
			Files.deleteIfExists(WORKING_DB);
			Files.copy(SNAPSHOT, WORKING_DB);
			sumsBeforeRecompute = StoredMetaSums.readFrom(WORKING_DB);
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
	void recomputesTheProductionSnapshotAndKeepsServingTheSummaries() {
		HttpResponse<String> response = Unirest.get("/api/v1/avatars?size=" + MAX_SIZE + "&token=test-token").asString();

		assertThat(response.getStatus()).isEqualTo(OK.getCode());
		JSONObject summaries = new JSONObject(response.getBody());
		assertThat(summaries.getLong("totalCount")).isPositive();
		assertThat(summaries.getJSONArray("items").length()).as("the artifact must carry every avatar, not a first page of them").isEqualTo(summaries.getInt("totalCount"));

		write("overview-after-recompute.json", response.getBody());
	}

	@Test
	void holdsEveryStoredMetaSumAgainstTheOneRecomputedFromTheSameRows() {
		assertThat(SNAPSHOT).as("the opt-in was given but no snapshot is there to measure").exists();

		MetaSumComparison comparison = new MetaSumComparison(sumsBeforeRecompute, StoredMetaSums.readFrom(WORKING_DB));

		write("metaSums-stored-vs-recomputed.tsv", comparison.asReport());

		assertThat(sumsBeforeRecompute.values()).as("a snapshot with no stored sums would make any diff vacuously empty").isNotEmpty();
		assertThat(comparison.comparedKeyCount())
				.as("every stored key must be held against a recomputed one, or the diff is measuring a failed read")
				.isEqualTo(sumsBeforeRecompute.values().size());
		assertThat(comparison.keysTheRecomputeNoLongerHolds()).as("the recompute must not drop a key it found stored").isEmpty();
	}

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
