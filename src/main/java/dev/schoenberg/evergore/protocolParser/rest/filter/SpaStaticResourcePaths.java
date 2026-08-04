package dev.schoenberg.evergore.protocolParser.rest.filter;

import jakarta.inject.Singleton;

@Singleton
public class SpaStaticResourcePaths {
	private static final String SHELL_PATH = "/";
	private static final String INDEX_PATH = "/index.html";
	private static final String ASSETS_PREFIX = "/assets/";

	private final PathCanonicalizer canonicalizer;

	public SpaStaticResourcePaths(PathCanonicalizer canonicalizer) {
		this.canonicalizer = canonicalizer;
	}

	public boolean matches(String rawPath) {
		String path = canonicalizer.canonicalize(rawPath);
		return path.equals(SHELL_PATH) || path.equals(INDEX_PATH) || path.startsWith(ASSETS_PREFIX);
	}
}
