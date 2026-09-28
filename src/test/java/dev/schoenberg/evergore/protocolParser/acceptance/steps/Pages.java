package dev.schoenberg.evergore.protocolParser.acceptance.steps;

import java.util.regex.Pattern;

final class Pages {
	static final String OVERVIEW = "/overview";
	static final String UNKNOWN = "/this-page-does-not-exist";
	static final Pattern NO_VIEW_FOR_THAT_LINK = Pattern.compile("Für .+ gibt es keine Ansicht\\.");

	private Pages() {}
}
