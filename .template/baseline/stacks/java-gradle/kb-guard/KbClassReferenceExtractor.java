package {{BASE_PACKAGE}}.kbguard;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class KbClassReferenceExtractor {
    record Candidate(int lineNumber, String token) {}

    private static final Pattern FENCE = Pattern.compile("^ {0,3}(```|~~~)");
    private static final Pattern HEADING = Pattern.compile("^ {0,3}#");
    private static final Pattern TABLE_ROW = Pattern.compile("^\\s*\\|");
    private static final Pattern HTML_COMMENT = Pattern.compile("^\\s*<!--");
    private static final Pattern LIST_ITEM = Pattern.compile("^\\s*(?:[-*+]|\\d+[.)])\\s");
    private static final Pattern LEADING_IDENTIFIER = Pattern.compile("[A-Z][A-Za-z0-9]*");
    private static final Pattern CAPITALIZED_HUMP = Pattern.compile("[A-Z][a-z0-9]*");

    private KbClassReferenceExtractor() {}

    static List<Candidate> extractCandidates(List<String> lines) {
        List<Candidate> candidates = new ArrayList<>();
        List<Integer> blockLineNumbers = new ArrayList<>();
        List<String> blockLines = new ArrayList<>();
        String openFence = null;
        for (int index = 0; index < lines.size(); index++) {
            int lineNumber = index + 1;
            String line = lines.get(index);
            Matcher fence = FENCE.matcher(line);
            boolean isFence = fence.find();
            if (openFence != null) {
                if (isFence && fence.group(1).equals(openFence)) {
                    openFence = null;
                } else {
                    scanBlock(List.of(lineNumber), List.of(line), candidates);
                }
            } else if (isFence) {
                flushBlock(blockLineNumbers, blockLines, candidates);
                openFence = fence.group(1);
            } else if (line.isBlank()) {
                flushBlock(blockLineNumbers, blockLines, candidates);
            } else if (HEADING.matcher(line).find() || TABLE_ROW.matcher(line).find() || HTML_COMMENT.matcher(line).find()) {
                flushBlock(blockLineNumbers, blockLines, candidates);
                scanBlock(List.of(lineNumber), List.of(line), candidates);
            } else {
                if (LIST_ITEM.matcher(line).find()) {
                    flushBlock(blockLineNumbers, blockLines, candidates);
                }
                blockLineNumbers.add(lineNumber);
                blockLines.add(line);
            }
        }
        flushBlock(blockLineNumbers, blockLines, candidates);
        return candidates;
    }

    private static void scanBlock(List<Integer> lineNumbers, List<String> blockLines, List<Candidate> candidates) {
        if (blockLines.isEmpty()) {
            return;
        }
        StringBuilder text = new StringBuilder();
        List<Integer> lineStarts = new ArrayList<>();
        for (String blockLine : blockLines) {
            lineStarts.add(text.length());
            text.append(blockLine).append(' ');
        }
        int position = 0;
        while (position < text.length()) {
            int open = text.indexOf("`", position);
            if (open < 0) {
                break;
            }
            int runLength = runLengthAt(text, open);
            int close = closingRunOfLength(text, open + runLength, runLength);
            if (close < 0) {
                position = open + runLength;
            } else {
                int openingLine = lineNumbers.get(lineIndexAt(lineStarts, open));
                candidateIn(spanContent(text.substring(open + runLength, close))).ifPresent(token -> candidates.add(new Candidate(openingLine, token)));
                position = close + runLength;
            }
        }
    }

    private static int runLengthAt(CharSequence text, int start) {
        int end = start;
        while (end < text.length() && text.charAt(end) == '`') {
            end++;
        }
        return end - start;
    }

    private static int closingRunOfLength(StringBuilder text, int from, int runLength) {
        int position = text.indexOf("`", from);
        while (position >= 0) {
            int length = runLengthAt(text, position);
            if (length == runLength) {
                return position;
            }
            position = text.indexOf("`", position + length);
        }
        return -1;
    }

    private static String spanContent(String raw) {
        boolean padded = raw.length() > 2 && raw.startsWith(" ") && raw.endsWith(" ") && !raw.isBlank();
        return padded ? raw.substring(1, raw.length() - 1) : raw;
    }

    private static void flushBlock(List<Integer> lineNumbers, List<String> blockLines, List<Candidate> candidates) {
        scanBlock(lineNumbers, blockLines, candidates);
        lineNumbers.clear();
        blockLines.clear();
    }

    private static int lineIndexAt(List<Integer> lineStarts, int offset) {
        int index = 0;
        while (index + 1 < lineStarts.size() && lineStarts.get(index + 1) <= offset) {
            index++;
        }
        return index;
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
