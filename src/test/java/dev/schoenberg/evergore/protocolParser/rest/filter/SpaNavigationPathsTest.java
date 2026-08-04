package dev.schoenberg.evergore.protocolParser.rest.filter;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SpaNavigationPathsTest {
	private final SpaNavigationPaths navigationPaths = new SpaNavigationPaths();

	@Test
	void ownsAnUnknownNavigationPath() {
		assertOwnedBySpa("/some/client/route");
	}

	@Test
	void ownsAClientRouteWithADotInAMiddleSegment() {
		assertOwnedBySpa("/avatars/Dr.Who/details");
	}

	@Test
	void ownsAClientRouteWhoseLastSegmentCarriesANonExtensionDot() {
		assertOwnedBySpa("/avatars/Dr.Who");
	}

	@Test
	void ownsAPathThatOnlyStartsWithTheLettersOfAReservedPrefix() {
		assertOwnedBySpa("/apiary/tour");
	}

	@Test
	void doesNotOwnAFileWithALowercaseExtension() {
		assertNotOwnedBySpa("/some/bundle.js");
	}

	@Test
	void doesNotOwnADotlessPathBelowTheAssetsMapping() {
		assertNotOwnedBySpa("/assets/bundle");
	}

	@Test
	void doesNotOwnTheApi() {
		assertNotOwnedBySpa("/api/v1/avatars");
	}

	@Test
	void doesNotOwnAReservedPrefixWithoutATrailingSegment() {
		assertNotOwnedBySpa("/api");
	}

	@Test
	void doesNotOwnTheSwaggerMapping() {
		assertNotOwnedBySpa("/swagger/does-not-exist");
	}

	@Test
	void doesNotOwnTheSwaggerUiMapping() {
		assertNotOwnedBySpa("/swagger-ui/does-not-exist");
	}

	@Test
	void doesNotOwnTheRedocMapping() {
		assertNotOwnedBySpa("/redoc/does-not-exist");
	}

	@Test
	void doesNotOwnTheRapidocMapping() {
		assertNotOwnedBySpa("/rapidoc/does-not-exist");
	}

	@Test
	void doesNotOwnTheHealthEndpoint() {
		assertNotOwnedBySpa("/health/does-not-exist");
	}

	private void assertOwnedBySpa(String canonicalPath) {
		boolean isSpaOwned = navigationPaths.isSpaOwned(canonicalPath);

		assertThat(isSpaOwned).as(canonicalPath + " should fall back to the SPA shell").isTrue();
	}

	private void assertNotOwnedBySpa(String canonicalPath) {
		boolean isSpaOwned = navigationPaths.isSpaOwned(canonicalPath);

		assertThat(isSpaOwned).as(canonicalPath + " should keep its 404").isFalse();
	}
}
