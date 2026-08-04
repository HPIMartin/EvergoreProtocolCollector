package dev.schoenberg.evergore.protocolParser.rest.filter;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PathCanonicalizerTest {
	private final PathCanonicalizer canonicalizer = new PathCanonicalizer();

	@Test
	void leavesAnAlreadyCanonicalPathUnchanged() {
		assertCanonical("/api/v1/avatars", "/api/v1/avatars");
	}

	@Test
	void mapsTheRootPathToItself() {
		assertCanonical("/", "/");
	}

	@Test
	void resolvesAParentSegment() {
		assertCanonical("/assets/../overview", "/overview");
	}

	@Test
	void resolvesAPercentEncodedParentSegment() {
		assertCanonical("/assets/%2e%2e/overview", "/overview");
	}

	@Test
	void resolvesAParentSegmentThatEndsThePath() {
		assertCanonical("/assets/..", "/");
	}

	@Test
	void clampsParentSegmentsAtTheRoot() {
		assertCanonical("/../../overview", "/overview");
	}

	@Test
	void dropsCurrentDirectorySegments() {
		assertCanonical("/assets/./app.js", "/assets/app.js");
	}

	@Test
	void collapsesEmptySegments() {
		assertCanonical("/assets//app.js", "/assets/app.js");
	}

	@Test
	void dropsATrailingSlash() {
		assertCanonical("/overview/", "/overview");
	}

	@Test
	void keepsADotInsideASegment() {
		assertCanonical("/avatars/Dr.Who", "/avatars/Dr.Who");
	}

	@Test
	void keepsADecodedSpaceInsideASegment() {
		assertCanonical("/avatars/Lady%20Aurora", "/avatars/Lady Aurora");
	}

	@Test
	void doesNotSwallowTheFirstSegmentOfADoubleSlashPathIntoAnAuthority() {
		assertCanonical("//api/v1/avatars", "/api/v1/avatars");
	}

	@Test
	void doesNotReduceADoubleSlashSingleSegmentPathToTheRoot() {
		assertCanonical("//overview", "/overview");
	}

	@Test
	void resolvesAParentSegmentHiddenBehindAnEncodedSeparator() {
		assertCanonical("/assets/%2f..%2foverview", "/overview");
	}

	@Test
	void keepsADoubleEncodedParentSegmentAsLiteralText() {
		assertCanonical("/assets/%252e%252e/overview", "/assets/%2e%2e/overview");
	}

	@Test
	void keepsAPlusInsideASegment() {
		assertCanonical("/avatars/Lady+Aurora", "/avatars/Lady+Aurora");
	}

	@Test
	void keepsASegmentWithAMalformedEscapeAsLiteralText() {
		assertCanonical("/overview%zz", "/overview%zz");
	}

	@Test
	void keepsASegmentEndingInALoneEscapeCharacterAsLiteralText() {
		assertCanonical("/overview%", "/overview%");
	}

	@Test
	void resolvesTheSegmentsAroundAMalformedEscape() {
		assertCanonical("/assets/../overview%zz", "/overview%zz");
	}

	private void assertCanonical(String rawPath, String expected) {
		String canonical = canonicalizer.canonicalize(rawPath);

		assertThat(canonical).as("canonical form of " + rawPath).isEqualTo(expected);
	}
}
