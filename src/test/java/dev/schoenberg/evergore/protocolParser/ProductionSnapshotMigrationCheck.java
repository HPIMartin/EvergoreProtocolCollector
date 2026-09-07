package dev.schoenberg.evergore.protocolParser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import dev.schoenberg.evergore.protocolParser.database.DatabaseMigration;
import dev.schoenberg.evergore.protocolParser.helper.config.Configuration;

import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.silentThrow;
import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfSystemProperty(named = "prodSnapshot.check", matches = "true", disabledReason = "on-demand: run with -DprodSnapshot.check=true and a local production snapshot")
class ProductionSnapshotMigrationCheck {
	private static final Path SNAPSHOT = Paths.get(System.getProperty("prodSnapshot.file", "temp.sqlite"));
	private static final Path WORKING_DB = Paths.get("build/tmp/prodSnapshotMigration/temp.sqlite");
	private static final String SEPARATOR = "~|~";

	private static final Map<String, String> EVERY_ROW = new LinkedHashMap<>();
	static {
		EVERY_ROW.put("bankEntries", "SELECT id, timeStamp, avatar, amount, type FROM bankEntries ORDER BY id");
		EVERY_ROW.put("storageEntries", "SELECT id, timeStamp, avatar, quantity, name, quality, type FROM storageEntries ORDER BY id");
		EVERY_ROW.put("metaInformation", "SELECT key, value FROM metaInformation ORDER BY key");
	}

	@Test
	void theMigrationKeepsEveryRowOfTheProductionSnapshotByteForByte() {
		silentThrow(() -> {
			assertThat(SNAPSHOT).as("the opt-in was given but no snapshot is there to migrate").exists();
			Files.createDirectories(WORKING_DB.getParent());
			Files.deleteIfExists(WORKING_DB);
			Files.copy(SNAPSHOT, WORKING_DB);
			return null;
		});

		Map<String, Integer> countsBefore = counts();
		Map<String, String> digestsBefore = digests();
		assertThat(countsBefore.values()).as("a snapshot with no rows would make any comparison vacuous").allSatisfy(count -> assertThat(count).isPositive());

		migrate();

		assertThat(counts()).isEqualTo(countsBefore);
		assertThat(digests()).isEqualTo(digestsBefore);
		assertThat(tableNames()).containsExactlyInAnyOrder("bankEntries", "flyway_schema_history", "metaInformation", "storageEntries");
		EVERY_ROW.keySet().forEach(table -> columnsOf(table).forEach(column -> assertThat(isNotNull(table, column)).describedAs("%s.%s is NOT NULL", table, column).isTrue()));

		migrate();

		assertThat(counts()).isEqualTo(countsBefore);
		assertThat(digests()).isEqualTo(digestsBefore);
	}

	private static void migrate() {
		new DatabaseMigration(configurationFor(WORKING_DB.toString()), new LoggerSpy()).migrate();
	}

	private static Map<String, Integer> counts() {
		Map<String, Integer> out = new LinkedHashMap<>();
		EVERY_ROW.keySet().forEach(table -> out.put(table, Integer.valueOf(query("SELECT count(*) FROM " + table).get(0))));
		return out;
	}

	private static Map<String, String> digests() {
		Map<String, String> out = new LinkedHashMap<>();
		EVERY_ROW.forEach((table, everyRow) -> out.put(table, digestOf(everyRow)));
		return out;
	}

	private static String digestOf(String everyRow) {
		return silentThrow(() -> {
			MessageDigest sha = MessageDigest.getInstance("SHA-256");
			for (String row : query(everyRow)) {
				sha.update((row + "\n").getBytes("UTF-8"));
			}
			StringBuilder hex = new StringBuilder();
			for (byte digested : sha.digest()) {
				hex.append(String.format("%02x", digested));
			}
			return hex.toString();
		});
	}

	private static List<String> tableNames() {
		return query("SELECT name FROM sqlite_master WHERE type='table' ORDER BY name");
	}

	private static List<String> columnsOf(String table) {
		return query("SELECT name FROM pragma_table_info('" + table + "')");
	}

	private static boolean isNotNull(String table, String column) {
		return "1".equals(query("SELECT \"notnull\" FROM pragma_table_info('" + table + "') WHERE name = '" + column + "'").get(0));
	}

	private static List<String> query(String sql) {
		return silentThrow(() -> {
			List<String> rows = new ArrayList<>();
			try (Connection c = connection(); Statement s = c.createStatement(); ResultSet r = s.executeQuery(sql)) {
				ResultSetMetaData meta = r.getMetaData();
				while (r.next()) {
					StringBuilder row = new StringBuilder();
					for (int column = 1; column <= meta.getColumnCount(); column++) {
						if (column > 1) {
							row.append(SEPARATOR);
						}
						row.append(r.getString(column));
					}
					rows.add(row.toString());
				}
			}
			return rows;
		});
	}

	private static Connection connection() throws Exception {
		return DriverManager.getConnection("jdbc:sqlite:" + WORKING_DB);
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
