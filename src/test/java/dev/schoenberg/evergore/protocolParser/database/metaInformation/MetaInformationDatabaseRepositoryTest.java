package dev.schoenberg.evergore.protocolParser.database.metaInformation;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.schoenberg.evergore.protocolParser.LoggerSpy;
import dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformation;
import dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey;
import dev.schoenberg.evergore.protocolParser.helper.config.Configuration;

import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.silentThrow;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MetaInformationDatabaseRepositoryTest {
	private static final String FRESH_DB_PATH = "build/tmp/test/metaInformationRepositoryTest.sqlite";

	@BeforeEach
	void deleteStaleDatabase() {
		silentThrow(() -> Files.deleteIfExists(Paths.get(FRESH_DB_PATH)));
	}

	@Test
	void storesEveryEntryOfABatch() {
		MetaInformationDatabaseRepository repo = repositoryOnAFreshFile();

		repo.add(List.of(entry("first", 1), entry("second", 2)));

		assertThat(storedRowCount()).isEqualTo(2);
	}

	@Test
	void storesNoEntryOfABatchWhoseLastOneCannotBeWritten() {
		MetaInformationDatabaseRepository repo = repositoryOnAFreshFile();
		List<MetaInformation<Integer>> batch = List.of(entry("first", 1), entry("second", 2), unwritableEntry("third"));

		assertThatThrownBy(() -> repo.add(batch)).isInstanceOf(RuntimeException.class);

		assertThat(storedRowCount()).isZero();
	}

	@Test
	void keepsAnEarlierValueOfAKeyWhoseBatchCannotBeWritten() {
		MetaInformationDatabaseRepository repo = repositoryOnAFreshFile();
		repo.add(List.of(entry("first", 1)));
		List<MetaInformation<Integer>> batch = List.of(entry("first", 99), unwritableEntry("second"));

		assertThatThrownBy(() -> repo.add(batch)).isInstanceOf(RuntimeException.class);

		assertThat(repo.get(new IntegerKey("first"))).contains(1);
	}

	private static MetaInformation<Integer> entry(String id, int value) {
		return new MetaInformation<>(new IntegerKey(id), value);
	}

	private static MetaInformation<Integer> unwritableEntry(String id) {
		return new MetaInformation<>(new UnwritableKey(id), 0);
	}

	private static long storedRowCount() {
		return silentThrow(() -> {
			try (Connection con = DriverManager.getConnection("jdbc:sqlite:" + FRESH_DB_PATH); Statement statement = con.createStatement()) {
				try (ResultSet rows = statement.executeQuery("SELECT COUNT(*) FROM " + MetaInformationEntry.TABLE)) {
					rows.next();
					return rows.getLong(1);
				}
			}
		});
	}

	private static MetaInformationDatabaseRepository repositoryOnAFreshFile() {
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

	private static class IntegerKey extends MetaInformationKey<Integer> {
		private IntegerKey(String id) {
			super(id);
		}

		@Override
		public String serialize(Integer value) {
			return String.valueOf(value);
		}

		@Override
		public Integer deserialize(String raw) {
			return Integer.valueOf(raw);
		}
	}

	private static class UnwritableKey extends IntegerKey {
		private UnwritableKey(String id) {
			super(id);
		}

		@Override
		public String serialize(Integer value) {
			throw new IllegalStateException("this key cannot be written");
		}
	}
}
