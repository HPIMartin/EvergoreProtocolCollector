package dev.schoenberg.evergore.protocolParser.rest.filter;

import java.net.URLDecoder;
import java.util.ArrayDeque;
import java.util.Deque;

import jakarta.inject.Singleton;

import static java.lang.String.join;
import static java.nio.charset.StandardCharsets.UTF_8;

@Singleton
public class PathCanonicalizer {
	private static final String SEPARATOR = "/";
	private static final String CURRENT_SEGMENT = ".";
	private static final String PARENT_SEGMENT = "..";
	private static final String ENCODED_PLUS = "%2B";

	public String canonicalize(String rawPath) {
		Deque<String> resolved = new ArrayDeque<>();
		for (String rawSegment : rawPath.split(SEPARATOR)) {
			for (String segment : decodeOnce(rawSegment).split(SEPARATOR)) {
				resolve(resolved, segment);
			}
		}
		return SEPARATOR + join(SEPARATOR, resolved);
	}

	private void resolve(Deque<String> resolved, String segment) {
		if (segment.isEmpty() || segment.equals(CURRENT_SEGMENT)) {
			return;
		}
		if (segment.equals(PARENT_SEGMENT)) {
			resolved.pollLast();
		} else {
			resolved.addLast(segment);
		}
	}

	private String decodeOnce(String rawSegment) {
		try {
			return URLDecoder.decode(rawSegment.replace("+", ENCODED_PLUS), UTF_8);
		} catch (IllegalArgumentException malformedEscape) {
			return rawSegment;
		}
	}
}
