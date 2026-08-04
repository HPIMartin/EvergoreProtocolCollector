package dev.schoenberg.evergore.protocolParser.rest.filter;

import java.util.List;

import jakarta.inject.Singleton;

import io.micronaut.core.util.AntPathMatcher;

import dev.schoenberg.evergore.protocolParser.helper.config.SecurityConfiguration;

@Singleton
public class PublicPaths {
	private final AntPathMatcher matcher = new AntPathMatcher();
	private final List<String> patterns;

	public PublicPaths(SecurityConfiguration securityConfiguration) {
		patterns = securityConfiguration.publicPaths() == null ? List.of() : securityConfiguration.publicPaths();
	}

	public boolean contains(String canonicalPath) {
		return patterns.stream().anyMatch(pattern -> matcher.matches(pattern, canonicalPath));
	}
}
