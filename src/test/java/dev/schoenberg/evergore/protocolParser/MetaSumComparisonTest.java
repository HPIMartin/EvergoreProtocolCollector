package dev.schoenberg.evergore.protocolParser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.silentThrow;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

class MetaSumComparisonTest {
	private static final Path DB = Paths.get("build/tmp/test/metaSumComparisonTest.sqlite");

	@Test
	void readsEveryStoredKeyAndItsValue() {
		StoredMetaSums stored = StoredMetaSums.readFrom(databaseHolding("bank_placement_Ada", "17", "last_updated", "31.07.2026 12:00"));

		assertThat(stored.values()).containsOnly(entry("bank_placement_Ada", "17"), entry("last_updated", "31.07.2026 12:00"));
	}

	@Test
	void leavesOutAKeyWhoseStoredValueIsNull() {
		StoredMetaSums stored = StoredMetaSums.readFrom(databaseHolding("intact", "7", "broken", null));

		assertThat(stored.values()).containsOnlyKeys("intact");
	}

	@Test
	void reportsAKeyWhoseNumberIsUnchangedAsUnchangedEvenWhenItsTextDiffers() {
		MetaSumComparison comparison = comparing(sums("storage_placement_Ada", "370.0"), sums("storage_placement_Ada", "370"));

		assertThat(comparison.differences()).isEmpty();
		assertThat(comparison.comparedKeyCount()).isEqualTo(1);
	}

	@Test
	void reportsAChangedNumberWithBothSidesAndTheirRatio() {
		MetaSumComparison comparison = comparing(sums("bank_placement_Ada", "50"), sums("bank_placement_Ada", "200"));

		assertThat(comparison.differences()).containsExactly(new MetaSumDifference("bank_placement_Ada", "50", "200", 4.0));
	}

	@Test
	void reportsNoRatioForAKeyThatWasStoredAtZero() {
		MetaSumComparison comparison = comparing(sums("storage_withdrawl_Ada", "0.0"), sums("storage_withdrawl_Ada", "1234.5"));

		assertThat(comparison.differences()).containsExactly(new MetaSumDifference("storage_withdrawl_Ada", "0.0", "1234.5", null));
	}

	@Test
	void reportsAKeyThatIsNotANumberOnEitherSideAsUnchanged() {
		MetaSumComparison comparison = comparing(sums("storage_placement_Ada", "NaN"), sums("storage_placement_Ada", "NaN"));

		assertThat(comparison.differences()).isEmpty();
	}

	@Test
	void reportsAKeyThatOnlyOneSideHolds() {
		MetaSumComparison comparison = comparing(sums("only_before", "1"), sums("only_after", "2"));

		assertThat(comparison.differences()).containsExactlyInAnyOrder(new MetaSumDifference("only_before", "1", null, null), new MetaSumDifference("only_after", null, "2", null));
	}

	@Test
	void countsOnlyTheKeysBothSidesHoldAsCompared() {
		MetaSumComparison comparison = comparing(sums("shared", "1", "only_before", "1"), sums("shared", "1"));

		assertThat(comparison.comparedKeyCount()).isEqualTo(1);
	}

	@Test
	void writesOneReportLinePerKeyUnderAHeader() {
		MetaSumComparison comparison = comparing(sums("bank_placement_Ada", "50", "shared", "1"), sums("bank_placement_Ada", "200", "shared", "1"));

		List<String> lines = comparison.asReport().lines().toList();

		assertThat(lines).hasSize(3);
		assertThat(lines.getFirst()).isEqualTo("key\tstored\trecomputed\tratio");
		assertThat(lines).contains("bank_placement_Ada\t50\t200\t4.0");
		assertThat(lines).contains("shared\t1\t1\t1.0");
	}

	private static MetaSumComparison comparing(StoredMetaSums stored, StoredMetaSums recomputed) {
		return new MetaSumComparison(stored, recomputed);
	}

	private static StoredMetaSums sums(String... keysAndValues) {
		Map<String, String> values = new LinkedHashMap<>();
		for (int i = 0; i < keysAndValues.length; i += 2) {
			values.put(keysAndValues[i], keysAndValues[i + 1]);
		}
		return new StoredMetaSums(values);
	}

	private static Path databaseHolding(String... keysAndValues) {
		return silentThrow(() -> {
			Files.createDirectories(DB.getParent());
			Files.deleteIfExists(DB);
			try (Connection con = DriverManager.getConnection("jdbc:sqlite:" + DB); Statement statement = con.createStatement()) {
				statement.executeUpdate("CREATE TABLE metaInformation (\"key\" VARCHAR, value VARCHAR, PRIMARY KEY (\"key\"))");
				for (int i = 0; i < keysAndValues.length; i += 2) {
					String value = keysAndValues[i + 1] == null ? "NULL" : "'" + keysAndValues[i + 1] + "'";
					statement.executeUpdate("INSERT INTO metaInformation (\"key\", value) VALUES ('" + keysAndValues[i] + "', " + value + ")");
				}
			}
			return DB;
		});
	}
}
