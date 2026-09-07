package dev.schoenberg.evergore.protocolParser.database;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.schoenberg.evergore.protocolParser.LoggerSpy;
import dev.schoenberg.evergore.protocolParser.helper.config.Configuration;

import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.silentThrow;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DatabaseMigrationTest {
	private static final Path DB = Paths.get("build/tmp/test/migrationTest.sqlite");

	private static final String LAX_BANK = "CREATE TABLE `bankEntries` (`id` VARCHAR , `timeStamp` VARCHAR , `avatar` VARCHAR , `amount` INTEGER , `type` VARCHAR , PRIMARY KEY (`id`) )";
	private static final String LAX_STORAGE = "CREATE TABLE `storageEntries` (`id` VARCHAR , `timeStamp` VARCHAR , `avatar` VARCHAR , `quantity` INTEGER , `name` VARCHAR , `quality` INTEGER , `type` VARCHAR , PRIMARY KEY (`id`) )";
	private static final String LAX_META = "CREATE TABLE `metaInformation` (`key` VARCHAR , `value` VARCHAR , PRIMARY KEY (`key`) )";

	private final LoggerSpy logger = new LoggerSpy();

	@BeforeEach
	void deleteStaleDatabase() {
		silentThrow(() -> {
			Files.createDirectories(DB.getParent());
			Files.deleteIfExists(DB);
		});
	}

	@Test
	void createsEveryTableOnAnEmptyDatabase() {
		migrate();

		assertThat(tableNames()).contains("bankEntries", "storageEntries", "metaInformation");
	}

	@Test
	void anEmptyDatabaseRefusesANullInEveryLedgerColumn() {
		migrate();

		assertRejectsNullInEveryColumn("bankEntries", "id, timeStamp, avatar, amount, type", List.of("'x'", "'2024-06-01 13:37:00.000000'", "'Aurora'", "1", "'EINLAGERUNG'"));
		assertRejectsNullInEveryColumn("storageEntries", "id, timeStamp, avatar, quantity, name, quality, type",
				List.of("'x'", "'2024-06-01 13:37:00.000000'", "'Aurora'", "1", "'Drachenhaut'", "80", "'EINLAGERUNG'"));
		assertRejectsNullInEveryColumn("metaInformation", "key, value", List.of("'k'", "'v'"));
	}

	@Test
	void anExistingDatabaseKeepsEveryRowThroughTheMigration() {
		createLaxSchema();
		execute("INSERT INTO bankEntries VALUES ('a', '2024-06-01 13:37:00.000000', 'Aurora', 1000, 'EINLAGERUNG')");
		execute("INSERT INTO storageEntries VALUES ('s', '2024-06-02 08:00:00.000000', 'Boreas', 3, 'Drachenhaut', 80, 'AUSLAGERUNG')");
		execute("INSERT INTO metaInformation VALUES ('lastUpdated', '2024-06-02 09:00:00')");
		List<String> before = allRows();

		migrate();

		assertThat(allRows()).isEqualTo(before);
	}

	@Test
	void anExistingDatabaseRefusesANullAfterTheMigration() {
		createLaxSchema();
		execute("INSERT INTO bankEntries VALUES ('a', '2024-06-01 13:37:00.000000', 'Aurora', 1000, 'EINLAGERUNG')");

		migrate();

		assertThatThrownBy(() -> execute("INSERT INTO bankEntries VALUES ('b', NULL, 'Boreas', 1, 'EINLAGERUNG')")).hasMessageContaining("NOT NULL constraint failed");
	}

	@Test
	void migratingTwiceLeavesTheSchemaAndTheRowsAlone() {
		createLaxSchema();
		execute("INSERT INTO bankEntries VALUES ('a', '2024-06-01 13:37:00.000000', 'Aurora', 1000, 'EINLAGERUNG')");
		migrate();
		List<String> afterFirst = allRows();

		migrate();

		assertThat(allRows()).isEqualTo(afterFirst);
		assertThatThrownBy(() -> execute("INSERT INTO bankEntries VALUES ('b', NULL, 'Boreas', 1, 'EINLAGERUNG')")).hasMessageContaining("NOT NULL constraint failed");
	}

	@Test
	void aDatabaseMissingOneOfTheTablesGetsItCreatedAndStillMigrates() {
		execute(LAX_BANK);
		execute(LAX_STORAGE);
		execute("INSERT INTO bankEntries VALUES ('a', '2024-06-01 13:37:00.000000', 'Aurora', 1000, 'EINLAGERUNG')");

		migrate();

		assertThat(tableNames()).contains("bankEntries", "storageEntries", "metaInformation");
		assertThat(allRows()).containsExactly("a|2024-06-01 13:37:00.000000|Aurora|1000|EINLAGERUNG|");
		assertRejectsNullInEveryColumn("metaInformation", "key, value", List.of("'k'", "'v'"));
	}

	@Test
	void aTableMissingAColumnAbortsTheMigrationInsteadOfDroppingIt() {
		execute("CREATE TABLE `bankEntries` (`id` VARCHAR , `timeStamp` VARCHAR , `avatar` VARCHAR , `amount` INTEGER , PRIMARY KEY (`id`) )");
		execute(LAX_STORAGE);
		execute(LAX_META);
		execute("INSERT INTO bankEntries VALUES ('a', '2024-06-01 13:37:00.000000', 'Aurora', 1000)");

		assertThatThrownBy(this::migrate).hasMessageContaining("no such column");

		assertThat(tableNames()).contains("bankEntries", "storageEntries", "metaInformation");
		assertThat(dump("SELECT id, timeStamp, avatar, amount FROM bankEntries", 4)).containsExactly("a|2024-06-01 13:37:00.000000|Aurora|1000|");
	}

	@Test
	void aPreExistingNullRowAbortsTheMigrationAndLeavesTheDatabaseRetryable() {
		createLaxSchema();
		execute("INSERT INTO bankEntries VALUES ('a', NULL, 'Aurora', 1000, 'EINLAGERUNG')");

		assertThatThrownBy(this::migrate).hasMessageContaining("NOT NULL constraint failed");

		assertThat(tableNames()).containsExactlyInAnyOrder("bankEntries", "flyway_schema_history", "metaInformation", "storageEntries");
		assertThat(allRows()).containsExactly("a|null|Aurora|1000|EINLAGERUNG|");

		execute("UPDATE bankEntries SET timeStamp = '2024-06-01 13:37:00.000000' WHERE id = 'a'");
		migrate();

		assertThat(allRows()).containsExactly("a|2024-06-01 13:37:00.000000|Aurora|1000|EINLAGERUNG|");
	}

	@Test
	void migratingFromSeveralThreadsAtOnceMigratesTheDatabaseExactlyOnce() {
		int threads = 8;
		CountDownLatch startTogether = new CountDownLatch(1);
		ExecutorService pool = Executors.newFixedThreadPool(threads);

		silentThrow(() -> {
			List<Callable<Void>> racers = new ArrayList<>();
			for (int racer = 0; racer < threads; racer++) {
				racers.add(() -> {
					startTogether.await();
					migrate();
					return null;
				});
			}
			List<Future<Void>> started = new ArrayList<>();
			for (Callable<Void> racer : racers) {
				started.add(pool.submit(racer));
			}
			startTogether.countDown();
			for (Future<Void> outcome : started) {
				outcome.get();
			}
			pool.shutdown();
			return null;
		});

		assertThat(tableNames()).containsExactlyInAnyOrder("bankEntries", "flyway_schema_history", "metaInformation", "storageEntries");
		assertRejectsNullInEveryColumn("metaInformation", "key, value", List.of("'k'", "'v'"));
	}

	private void assertRejectsNullInEveryColumn(String table, String columns, List<String> values) {
		List<String> columnNames = List.of(columns.split(",\\s*"));
		for (int nulled = 0; nulled < columnNames.size(); nulled++) {
			List<String> row = new ArrayList<>(values);
			row.set(nulled, "NULL");
			String insert = "INSERT INTO " + table + " (" + columns + ") VALUES (" + String.join(", ", row) + ")";

			assertThatThrownBy(() -> execute(insert))
					.describedAs("a NULL in %s.%s must be refused", table, columnNames.get(nulled))
					.hasMessageContaining("NOT NULL constraint failed");
		}
	}

	private void migrate() {
		new DatabaseMigration(configurationFor(DB.toString()), logger).migrate();
	}

	private void createLaxSchema() {
		execute(LAX_BANK);
		execute(LAX_STORAGE);
		execute(LAX_META);
	}

	private static List<String> allRows() {
		List<String> rows = new ArrayList<>();
		rows.addAll(dump("SELECT id, timeStamp, avatar, amount, type FROM bankEntries ORDER BY id", 5));
		rows.addAll(dump("SELECT id, timeStamp, avatar, quantity, name, quality, type FROM storageEntries ORDER BY id", 7));
		rows.addAll(dump("SELECT key, value FROM metaInformation ORDER BY key", 2));
		return rows;
	}

	private static List<String> dump(String query, int columns) {
		return silentThrow(() -> {
			List<String> rows = new ArrayList<>();
			try (Connection c = connection(); Statement s = c.createStatement(); ResultSet r = s.executeQuery(query)) {
				while (r.next()) {
					StringBuilder row = new StringBuilder();
					for (int column = 1; column <= columns; column++) {
						row.append(r.getString(column)).append("|");
					}
					rows.add(row.toString());
				}
			}
			return rows;
		});
	}

	private static List<String> tableNames() {
		return dump("SELECT name FROM sqlite_master WHERE type='table' ORDER BY name", 1).stream().map(name -> name.replace("|", "")).toList();
	}

	private static void execute(String sql) {
		silentThrow(() -> {
			try (Connection c = connection(); Statement s = c.createStatement()) {
				s.executeUpdate(sql);
			}
			return null;
		});
	}

	private static Connection connection() throws SQLException {
		return DriverManager.getConnection("jdbc:sqlite:" + DB);
	}

	private static Configuration configurationFor(String databasePath) {
		return new Configuration() {
			@Override
			public String getDatabasePath() {
				return databasePath;
			}
		};
	}
}
