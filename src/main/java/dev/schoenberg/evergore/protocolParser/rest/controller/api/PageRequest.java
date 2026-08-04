package dev.schoenberg.evergore.protocolParser.rest.controller.api;

public record PageRequest(int page, int size) {
	public static final String PAGE = "page";
	public static final String SIZE = "size";
	public static final String DEFAULT_PAGE = "0";
	public static final String DEFAULT_SIZE = "100";
	public static final int MAX_SIZE = 1000;

	public long offset() {
		return (long) page * size;
	}
}
