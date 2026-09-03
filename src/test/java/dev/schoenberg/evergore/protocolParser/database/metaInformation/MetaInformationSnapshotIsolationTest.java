package dev.schoenberg.evergore.protocolParser.database.metaInformation;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.schoenberg.evergore.protocolParser.LoggerSpy;
import dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformation;
import dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey;
import dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationSnapshot;
import dev.schoenberg.evergore.protocolParser.helper.config.Configuration;

import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.silentThrow;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;

class MetaInformationSnapshotIsolationTest {
	private static final String FRESH_DB_PATH = "build/tmp/test/metaInformationIsolationTest.sqlite";
	private static final List<String> KEYS = List.of("first", "second", "third", "fourth");
	private static final long HANG_GUARD_SECONDS = 30;

	private final CountDownLatch recomputeIsInsideItsTransaction = new CountDownLatch(1);
	private final CountDownLatch readerHasTakenItsSnapshot = new CountDownLatch(1);

	@BeforeEach
	void deleteStaleDatabase() {
		silentThrow(() -> Files.deleteIfExists(Paths.get(FRESH_DB_PATH)));
	}

	@Test
	void answersTheStateBeforeARecomputeWhileThatRecomputeIsStillOpen() throws Exception {
		MetaInformationDatabaseRepository writer = repositoryOnTheFreshFile();
		writer.add(generation(1));
		MetaInformationDatabaseRepository reader = repositoryOnTheFreshFile();

		Thread recompute = new Thread(() -> writer.add(generationBlockingOnTheReader(2)));
		recompute.start();
		assertThat(recomputeIsInsideItsTransaction.await(HANG_GUARD_SECONDS, TimeUnit.SECONDS)).as("the recompute never entered its transaction").isTrue();

		Set<Long> readDuringTheRecompute = generationsIn(reader.snapshot());
		readerHasTakenItsSnapshot.countDown();
		recompute.join();

		assertThat(readDuringTheRecompute).as("a read landing inside an open recompute must see the state before it, whole").containsExactly(1L);
		assertThat(generationsIn(reader.snapshot())).as("once the recompute committed, the next read sees it whole").containsExactly(2L);
	}

	private List<MetaInformation<Long>> generation(long value) {
		return KEYS.stream().map(id -> new MetaInformation<>(new LongKey(id), value)).toList();
	}

	private List<MetaInformation<Long>> generationBlockingOnTheReader(long value) {
		return Stream
				.of(Stream.of(new MetaInformation<>(new LongKey(KEYS.get(0)), value)), Stream.of(new MetaInformation<>(new BlockingLongKey(KEYS.get(1)), value)),
						generation(value).stream().skip(2))
				.flatMap(keys -> keys)
				.toList();
	}

	private static Set<Long> generationsIn(MetaInformationSnapshot snapshot) {
		return KEYS.stream().map(id -> snapshot.get(new LongKey(id)).orElseThrow()).collect(toSet());
	}

	private static MetaInformationDatabaseRepository repositoryOnTheFreshFile() {
		return MetaInformationDatabaseRepository.get(configurationFor(FRESH_DB_PATH), new LoggerSpy(), () -> {});
	}

	private static Configuration configurationFor(String databasePath) {
		return new Configuration() {
			@Override
			public String getDatabasePath() {
				return databasePath;
			}
		};
	}

	private static class LongKey extends MetaInformationKey<Long> {
		private LongKey(String id) {
			super(id);
		}

		@Override
		public String serialize(Long value) {
			return String.valueOf(value);
		}

		@Override
		public Long deserialize(String raw) {
			return Long.valueOf(raw);
		}
	}

	private class BlockingLongKey extends LongKey {
		private BlockingLongKey(String id) {
			super(id);
		}

		@Override
		public String serialize(Long value) {
			recomputeIsInsideItsTransaction.countDown();
			silentThrow(() -> readerHasTakenItsSnapshot.await(HANG_GUARD_SECONDS, TimeUnit.SECONDS));
			return super.serialize(value);
		}
	}
}
