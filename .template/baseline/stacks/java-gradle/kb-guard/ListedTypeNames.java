package {{BASE_PACKAGE}}.kbguard;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class ListedTypeNames {
    private static final String BLOCK_START = "- **DOC-12 ";
    private static final String EXAMPLE_HEADER = "  - Example list:";
    private static final String STOP_HEADER = "  - Stop list:";
    private static final String SUB_BULLET = "  - ";
    private static final String ENTRY_PREFIX = "    - ";
    private static final String CONTINUATION_INDENT = "      ";
    private static final String DOTTED_ACCESS = ".member";
    private static final Pattern ENTRY = Pattern.compile("^    - `([^`]+)`: (\\S.*)$");

    private final Map<String, String> examples;
    private final Map<String, String> stops;

    private ListedTypeNames(Map<String, String> examples, Map<String, String> stops) {
        this.examples = examples;
        this.stops = stops;
    }

    static ListedTypeNames parse(List<String> readmeLines, String source) {
        int start = indexOfBlockStart(readmeLines, source);
        Map<String, String> examples = new LinkedHashMap<>();
        Map<String, String> stops = new LinkedHashMap<>();
        boolean exampleHeaderSeen = false;
        boolean stopHeaderSeen = false;
        Map<String, String> current = null;
        String lastName = null;
        int index = start + 1;
        for (; index < readmeLines.size() && !endsBlock(readmeLines.get(index)); index++) {
            String line = readmeLines.get(index);
            if (line.stripTrailing().equals(EXAMPLE_HEADER)) {
                exampleHeaderSeen = true;
                current = examples;
                lastName = null;
            } else if (line.stripTrailing().equals(STOP_HEADER)) {
                stopHeaderSeen = true;
                current = stops;
                lastName = null;
            } else if (line.startsWith(SUB_BULLET)) {
                current = null;
                lastName = null;
            } else if (current != null && line.startsWith(ENTRY_PREFIX)) {
                Matcher entry = ENTRY.matcher(line);
                if (!entry.matches()) {
                    throw new IllegalStateException(source + ":" + (index + 1) + ": list entry without a reason: " + line.strip());
                }
                lastName = entry.group(1);
                if (!isYieldedByTheCandidateRule(lastName)) {
                    throw new IllegalStateException(source + ":" + (index + 1) + ": list entry is not a name the candidate rule yields: " + lastName);
                }
                current.put(lastName, entry.group(2).strip());
            } else if (current != null && lastName != null && line.startsWith(CONTINUATION_INDENT)) {
                current.put(lastName, current.get(lastName) + " " + line.strip());
            }
        }
        if (!exampleHeaderSeen) {
            throw missingHeader(source, "Example list:", readmeLines, index);
        }
        if (!stopHeaderSeen) {
            throw missingHeader(source, "Stop list:", readmeLines, index);
        }
        return new ListedTypeNames(Collections.unmodifiableMap(examples), Collections.unmodifiableMap(stops));
    }

    private static IllegalStateException missingHeader(String source, String header, List<String> readmeLines, int blockEnd) {
        if (blockEnd < readmeLines.size()) {
            return new IllegalStateException(source + ": the DOC-12 block ends at " + source + ":" + (blockEnd + 1) + " (`" + readmeLines.get(blockEnd).strip() + "`) before its '" + header + "' header");
        }
        return new IllegalStateException(source + ": the DOC-12 block has no '" + header + "' header");
    }

    private static boolean isYieldedByTheCandidateRule(String name) {
        return isYieldedFrom("`" + name + "`", name) || isYieldedFrom("`" + name + DOTTED_ACCESS + "`", name);
    }

    private static boolean isYieldedFrom(String span, String name) {
        List<KbClassReferenceExtractor.Candidate> yielded = KbClassReferenceExtractor.extractCandidates(List.of(span));
        return yielded.size() == 1 && yielded.get(0).token().equals(name);
    }

    private static int indexOfBlockStart(List<String> readmeLines, String source) {
        for (int index = 0; index < readmeLines.size(); index++) {
            if (readmeLines.get(index).startsWith(BLOCK_START)) {
                return index;
            }
        }
        throw new IllegalStateException(source + ": no DOC-12 block (a line beginning '" + BLOCK_START + "')");
    }

    private static boolean endsBlock(String line) {
        return !line.isBlank() && !Character.isWhitespace(line.charAt(0));
    }

    Map<String, String> examples() {
        return examples;
    }

    Map<String, String> stops() {
        return stops;
    }

    boolean contains(String name) {
        return examples.containsKey(name) || stops.containsKey(name);
    }
}
