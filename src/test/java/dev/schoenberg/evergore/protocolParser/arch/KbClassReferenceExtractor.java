package dev.schoenberg.evergore.protocolParser.arch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class KbClassReferenceExtractor {

	static final Map<String, String> STOPLISTED_TOKEN_REASONS = buildStoplistedTokenReasons();

	private static Map<String, String> buildStoplistedTokenReasons() {
		Map<String, String> reasons = new HashMap<>();
		reasons.put("ArchUnit", "product/brand name, no literal class of that name");
		reasons.put("NeedBraces", "Checkstyle rule short-name (the real class is NeedBracesCheck); Checkstyle is a Gradle plugin, not on the application's compile/test classpath");
		reasons.put("AvoidStarImport", "Checkstyle rule short-name (the real class is AvoidStarImportCheck); Checkstyle is a Gradle plugin, not on the classpath");
		reasons.put("JavaCompile", "Gradle API class, only on the build script's classpath, not the application's");
		reasons.put("EnterWorktree", "a Claude Code tool name, not a project class");
		reasons.put("D2AcceptanceTest", "the engineering handbook's own deliberate counter-example of a bad class name, not a claim it exists");
		reasons.put("SonarSource", "VS Code extension publisher id, not a class");
		reasons.put("StrictHostKeyChecking", "OpenSSH client option name, not a class");
		return Collections.unmodifiableMap(reasons);
	}

	private static final Pattern BACKTICK_SPAN = Pattern.compile("`([^`\\n]+)`");
	private static final Pattern LEADING_IDENTIFIER = Pattern.compile("[A-Z][A-Za-z0-9]*");
	private static final Pattern CAPITALIZED_HUMP = Pattern.compile("[A-Z][a-z0-9]*");

	private KbClassReferenceExtractor() {}

	static List<String> extractCandidates(String text) {
		List<String> candidates = new ArrayList<>();
		Matcher spans = BACKTICK_SPAN.matcher(text);
		while (spans.find()) {
			candidateIn(spans.group(1)).ifPresent(candidates::add);
		}
		return candidates;
	}

	private static Optional<String> candidateIn(String backtickSpanContent) {
		Matcher leading = LEADING_IDENTIFIER.matcher(backtickSpanContent);
		if (!leading.lookingAt()) {
			return Optional.empty();
		}

		String token = leading.group();
		boolean isDottedMemberAccess = backtickSpanContent.length() > token.length() && backtickSpanContent.charAt(token.length()) == '.';

		if (!hasLowercaseLetter(token)) {
			return Optional.empty();
		}
		if (!isDottedMemberAccess && capitalizedHumpCount(token) < 2) {
			return Optional.empty();
		}
		if (STOPLISTED_TOKEN_REASONS.containsKey(token)) {
			return Optional.empty();
		}
		return Optional.of(token);
	}

	private static boolean hasLowercaseLetter(String token) {
		return token.chars().anyMatch(Character::isLowerCase);
	}

	private static int capitalizedHumpCount(String token) {
		Matcher humps = CAPITALIZED_HUMP.matcher(token);
		int count = 0;
		while (humps.find()) {
			count++;
		}
		return count;
	}
}
