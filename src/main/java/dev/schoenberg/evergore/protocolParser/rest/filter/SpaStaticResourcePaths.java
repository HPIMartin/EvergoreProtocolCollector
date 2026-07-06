package dev.schoenberg.evergore.protocolParser.rest.filter;

public final class SpaStaticResourcePaths {
	private static final String ASSETS_PREFIX = "/assets/";

	private SpaStaticResourcePaths() {}

	public static boolean matches(String path) {
		return path.equals("/") || path.equals("/index.html") || path.startsWith(ASSETS_PREFIX);
	}
}
