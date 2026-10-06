package dev.schoenberg.evergore.protocolParser.database;

public record SortColumn(String name, boolean inGermanOrder) {
	public static SortColumn of(String name) {
		return new SortColumn(name, false);
	}

	public static SortColumn germanOrderOf(String name) {
		return new SortColumn(name, true);
	}
}
