package dev.schoenberg.evergore.protocolParser.rest.filter;

import java.util.List;

import org.junit.jupiter.api.Test;

import dev.schoenberg.evergore.protocolParser.helper.config.SecurityConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

class PublicPathsTest {
	private static final List<String> CONFIGURED = List.of("/", "/index.html", "/assets/**", "/health");

	@Test
	void containsAnExactlyConfiguredPath() {
		assertPublic("/index.html");
	}

	@Test
	void containsTheConfiguredRoot() {
		assertPublic("/");
	}

	@Test
	void containsAPathBelowAConfiguredWildcard() {
		assertPublic("/assets/index-abc123.js");
	}

	@Test
	void containsAPathNestedDeeperBelowAConfiguredWildcard() {
		assertPublic("/assets/fonts/inter.woff2");
	}

	@Test
	void doesNotContainAnUnconfiguredPath() {
		assertNotPublic("/i_dont_exist");
	}

	@Test
	void doesNotContainTheApi() {
		assertNotPublic("/api/v1/avatars");
	}

	@Test
	void doesNotContainAPathThatOnlyExtendsTheLettersOfAConfiguredPath() {
		assertNotPublic("/healthz");
	}

	@Test
	void doesNotContainTheWildcardPrefixItselfWithoutItsSeparator() {
		assertNotPublic("/assetsomething");
	}

	@Test
	void doesNotContainAnythingWhenNoPathIsConfigured() {
		PublicPaths publicPaths = new PublicPaths(new SecurityConfiguration("token", List.of()));

		boolean isPublic = publicPaths.contains("/");

		assertThat(isPublic).as("an empty configuration must protect everything").isFalse();
	}

	@Test
	void doesNotContainAnythingWhenTheConfigurationIsMissing() {
		PublicPaths publicPaths = new PublicPaths(new SecurityConfiguration("token", null));

		boolean isPublic = publicPaths.contains("/");

		assertThat(isPublic).as("a missing configuration must protect everything").isFalse();
	}

	private static void assertPublic(String canonicalPath) {
		boolean isPublic = configuredPaths().contains(canonicalPath);

		assertThat(isPublic).as(canonicalPath + " must be reachable without a token").isTrue();
	}

	private static void assertNotPublic(String canonicalPath) {
		boolean isPublic = configuredPaths().contains(canonicalPath);

		assertThat(isPublic).as(canonicalPath + " must require a token").isFalse();
	}

	private static PublicPaths configuredPaths() {
		return new PublicPaths(new SecurityConfiguration("token", CONFIGURED));
	}
}
