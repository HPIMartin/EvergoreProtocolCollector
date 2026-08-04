package dev.schoenberg.evergore.protocolParser.rest.filter;

import java.util.List;
import java.util.regex.Pattern;

import jakarta.inject.Singleton;

@Singleton
public class SpaNavigationPaths {
	private static final List<String> RESERVED_PREFIXES = List.of("/api", "/assets", "/health", "/rapidoc", "/redoc", "/swagger", "/swagger-ui");
	private static final Pattern LOWERCASE_FILE_EXTENSION = Pattern.compile("\\.[a-z0-9]{1,8}$");
	private static final char SEPARATOR = '/';

	public boolean isSpaOwned(String canonicalPath) {
		return !isReserved(canonicalPath) && !namesAFile(canonicalPath);
	}

	private boolean isReserved(String canonicalPath) {
		return RESERVED_PREFIXES.stream().anyMatch(prefix -> canonicalPath.equals(prefix) || canonicalPath.startsWith(prefix + SEPARATOR));
	}

	private boolean namesAFile(String canonicalPath) {
		return LOWERCASE_FILE_EXTENSION.matcher(lastSegmentOf(canonicalPath)).find();
	}

	private String lastSegmentOf(String canonicalPath) {
		return canonicalPath.substring(canonicalPath.lastIndexOf(SEPARATOR) + 1);
	}
}
