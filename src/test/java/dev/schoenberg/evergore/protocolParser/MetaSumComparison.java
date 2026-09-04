package dev.schoenberg.evergore.protocolParser;

import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;

import static java.lang.String.valueOf;

public record MetaSumComparison(StoredMetaSums stored, StoredMetaSums recomputed) {
	private static final String REPORT_HEADER = "key\tstored\trecomputed\tratio";

	public List<MetaSumDifference> differences() {
		return everyKey().stream().map(this::compare).filter(difference -> !unchanged(difference)).toList();
	}

	public long comparedKeyCount() {
		return everyKey().stream().filter(key -> stored.values().containsKey(key) && recomputed.values().containsKey(key)).count();
	}

	public List<String> keysTheRecomputeNoLongerHolds() {
		return everyKey().stream().filter(key -> !recomputed.values().containsKey(key)).toList();
	}

	public String asReport() {
		StringBuilder report = new StringBuilder(REPORT_HEADER).append('\n');
		for (MetaSumDifference difference : everyKey().stream().map(this::compare).toList()) {
			report
					.append(difference.key())
					.append('\t')
					.append(text(difference.stored()))
					.append('\t')
					.append(text(difference.recomputed()))
					.append('\t')
					.append(difference.ratio() == null ? "" : valueOf(difference.ratio()))
					.append('\n');
		}
		return report.toString();
	}

	private MetaSumDifference compare(String key) {
		String storedValue = stored.values().get(key);
		String recomputedValue = recomputed.values().get(key);
		return new MetaSumDifference(key, storedValue, recomputedValue, ratioOf(storedValue, recomputedValue));
	}

	private boolean unchanged(MetaSumDifference difference) {
		if (difference.stored() == null || difference.recomputed() == null) {
			return false;
		}
		Optional<Double> storedNumber = asNumber(difference.stored());
		Optional<Double> recomputedNumber = asNumber(difference.recomputed());
		if (storedNumber.isPresent() && recomputedNumber.isPresent()) {
			return sameNumber(storedNumber.get(), recomputedNumber.get());
		}
		return difference.stored().equals(difference.recomputed());
	}

	private Double ratioOf(String storedValue, String recomputedValue) {
		if (storedValue == null || recomputedValue == null) {
			return null;
		}
		Optional<Double> storedNumber = asNumber(storedValue);
		Optional<Double> recomputedNumber = asNumber(recomputedValue);
		if (storedNumber.isEmpty() || recomputedNumber.isEmpty() || storedNumber.get().doubleValue() == 0.0) {
			return null;
		}
		return recomputedNumber.get() / storedNumber.get();
	}

	private static boolean sameNumber(double stored, double recomputed) {
		return stored == recomputed || Double.isNaN(stored) && Double.isNaN(recomputed);
	}

	private static Optional<Double> asNumber(String value) {
		try {
			return Optional.of(Double.valueOf(value));
		} catch (NumberFormatException notANumber) {
			return Optional.empty();
		}
	}

	private static String text(String value) {
		return value == null ? "" : value;
	}

	private SortedSet<String> everyKey() {
		SortedSet<String> keys = new TreeSet<>(stored.values().keySet());
		keys.addAll(recomputed.values().keySet());
		return keys;
	}
}
