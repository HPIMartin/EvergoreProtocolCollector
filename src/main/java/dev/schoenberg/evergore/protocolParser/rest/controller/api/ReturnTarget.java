package dev.schoenberg.evergore.protocolParser.rest.controller.api;

import java.net.URLDecoder;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;

final class ReturnTarget {
	static final String OVERVIEW = "/";

	private static final char DELETE = 0x7f;

	private ReturnTarget() {}

	static String of(String next) {
		if (next == null) {
			return OVERVIEW;
		}
		Optional<String> decoded = decodedOnce(next);
		if (decoded.isEmpty() || !isAPathOfThisService(next) || !isAPathOfThisService(decoded.get())) {
			return OVERVIEW;
		}
		return next;
	}

	private static boolean isAPathOfThisService(String candidate) {
		String slashed = candidate.replace('\\', '/');
		return slashed.startsWith("/") && !slashed.startsWith("//") && slashed.chars().noneMatch(ReturnTarget::isAControlCharacter);
	}

	private static boolean isAControlCharacter(int character) {
		return character < ' ' || character == DELETE;
	}

	private static Optional<String> decodedOnce(String next) {
		try {
			return Optional.of(URLDecoder.decode(next, UTF_8));
		} catch (IllegalArgumentException malformed) {
			return Optional.empty();
		}
	}
}
