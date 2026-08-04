package dev.schoenberg.evergore.protocolParser.rest.filter;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SpaStaticResourcePathsTest {
	private final SpaStaticResourcePaths staticResourcePaths = new SpaStaticResourcePaths(new PathCanonicalizer());

	@Test
	void matchesTheSpaShell() {
		assertMatches("/");
	}

	@Test
	void matchesTheSpaIndex() {
		assertMatches("/index.html");
	}

	@Test
	void matchesABundledAsset() {
		assertMatches("/assets/index-abc123.js");
	}

	@Test
	void doesNotMatchATraversalThatLeavesTheAssetsMapping() {
		assertDoesNotMatch("/assets/../overview");
	}

	@Test
	void doesNotMatchAPercentEncodedTraversalThatLeavesTheAssetsMapping() {
		assertDoesNotMatch("/assets/%2e%2e/overview");
	}

	@Test
	void doesNotMatchAProtectedPage() {
		assertDoesNotMatch("/overview");
	}

	@Test
	void doesNotMatchADoubleSlashPathAsTheSpaRoot() {
		assertDoesNotMatch("//overview");
	}

	private void assertMatches(String rawPath) {
		boolean matches = staticResourcePaths.matches(rawPath);

		assertThat(matches).as(rawPath + " is a static SPA resource").isTrue();
	}

	private void assertDoesNotMatch(String rawPath) {
		boolean matches = staticResourcePaths.matches(rawPath);

		assertThat(matches).as(rawPath + " is not a static SPA resource").isFalse();
	}
}
