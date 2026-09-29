package dev.schoenberg.evergore.protocolParser.acceptance.operator;

import java.util.Arrays;
import java.util.Optional;

public enum FindingList {
	UNKNOWN_ITEMS("the unknown items", "Unbekannte Items", "unknownItemNames"),
	WORTHLESS_ITEMS("the items worth nothing", "zeroValuedItemNames"),
	UNREFRESHED_MEMBERS("the members whose figures could not be refreshed", "Nicht aktualisierte Avatare", "failedAvatarNames"),
	ROUND_TRIPS("the suspected round trips", "Verdacht auf Warenkreislauf", "roundTrips"),
	NOT_JUDGEABLE("the pairs not judgeable", "Nicht beurteilbar (Rezept ungelesen)", "roundTripAbstentions");

	public static final String NAMES = "the unknown items|the items worth nothing|the members whose figures could not be refreshed|the suspected round trips|the pairs not judgeable";

	private final String phrase;
	private final Optional<String> heading;
	private final String detail;

	FindingList(String phrase, String heading, String detail) {
		this(phrase, Optional.of(heading), detail);
	}

	FindingList(String phrase, String detail) {
		this(phrase, Optional.empty(), detail);
	}

	FindingList(String phrase, Optional<String> heading, String detail) {
		this.phrase = phrase;
		this.heading = heading;
		this.detail = detail;
	}

	public static FindingList named(String phrase) {
		return Arrays
				.stream(values())
				.filter(list -> list.phrase.equals(phrase))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("No finding list is named \"" + phrase + "\""));
	}

	Optional<String> heading() {
		return heading;
	}

	String detail() {
		return detail;
	}
}
