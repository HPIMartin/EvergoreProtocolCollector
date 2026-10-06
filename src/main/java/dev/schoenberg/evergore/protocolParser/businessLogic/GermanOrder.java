package dev.schoenberg.evergore.protocolParser.businessLogic;

import java.text.Collator;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class GermanOrder {
	private static final Collator COLLATOR = Collator.getInstance(Locale.GERMANY);

	public static final Comparator<String> NAMES = COLLATOR::compare;

	private GermanOrder() {}

	public static Collator ownCollator() {
		return Collator.getInstance(Locale.GERMANY);
	}

	public static List<String> distinctSorted(List<String> names) {
		return names.stream().distinct().sorted(NAMES).toList();
	}
}
