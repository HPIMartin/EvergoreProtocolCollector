package {{BASE_PACKAGE}}.kbguard;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class KbClassReferenceExtractorTest {
    @Test
    void extractsACompoundPascalCaseNameFromBackticks() {
        List<String> extracted = tokensOf("See `PriceCalculator` for details.");

        assertThat(extracted).containsExactly("PriceCalculator");
    }

    @Test
    void extractsEveryCompoundNameOfALineInOrder() {
        List<String> extracted = tokensOf("`OrderBook` feeds `PriceCalculator`.");

        assertThat(extracted).containsExactly("OrderBook", "PriceCalculator");
    }

    @Test
    void ignoresACompoundNameOutsideBackticks() {
        List<String> extracted = tokensOf("See PriceCalculator for details.");

        assertThat(extracted).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({"`Configuration.browser`,Configuration", "`Browser.DOCKER`,Browser"})
    void extractsOnlyTheLeadingSegmentOfADottedMemberAccess(String backtickSpan, String expectedLeadingSegment) {
        List<String> extracted = tokensOf(backtickSpan);

        assertThat(extracted).containsExactly(expectedLeadingSegment);
    }

    @ParameterizedTest
    @ValueSource(strings = {"`CRLF`", "`D5`"})
    void excludesATokenWithNoLowercaseLetter(String backtickSpan) {
        List<String> extracted = tokensOf(backtickSpan);

        assertThat(extracted).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"`Invoice`", "`Deposit`", "`Agent`"})
    void excludesASingleWordTokenWithFewerThanTwoCapitalizedHumps(String backtickSpan) {
        List<String> extracted = tokensOf(backtickSpan);

        assertThat(extracted).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"`-o Name=value`", "`git log`", "`./verify all`", "`transferType`", "`myPhantomCamelName`"})
    void excludesAnOptionWithADashACommandAndALowercaseName(String backtickSpan) {
        List<String> extracted = tokensOf(backtickSpan);

        assertThat(extracted).isEmpty();
    }

    @Test
    void extractsAMultiHumpTokenWithADigitInAHump() {
        List<String> extracted = tokensOf("`TokenScope2Test` proves the default-deny scope.");

        assertThat(extracted).containsExactly("TokenScope2Test");
    }

    @Test
    void readsASpanThatWrapsOntoTheNextLineAndTheNameAfterIt() {
        List<KbClassReferenceExtractor.Candidate> extracted = candidatesOf(
                "Line one opens a span `git merge --ff-only",
                "refs/remotes/origin/main` and then cites `PhantomAfterWrappedSpan` here.");

        assertThat(extracted).containsExactly(new KbClassReferenceExtractor.Candidate(2, "PhantomAfterWrappedSpan"));
    }

    @Test
    void reportsASpanAtTheLineWhereItOpens() {
        List<KbClassReferenceExtractor.Candidate> extracted = candidatesOf(
                "first",
                "`WrapOpener",
                "closes here` and `SecondName` follows");

        assertThat(extracted).containsExactly(
                new KbClassReferenceExtractor.Candidate(2, "WrapOpener"),
                new KbClassReferenceExtractor.Candidate(3, "SecondName"));
    }

    @Test
    void countsALineBreakInsideASpanAsASpace() {
        List<String> extracted = tokensOf("`Configuration", ".browser`");

        assertThat(extracted).isEmpty();
    }

    @Test
    void letsASpanWrapInsideAListItem() {
        List<KbClassReferenceExtractor.Candidate> extracted = candidatesOf("- item text `PhantomOpener", "continues` here");

        assertThat(extracted).containsExactly(new KbClassReferenceExtractor.Candidate(1, "PhantomOpener"));
    }

    @Test
    void dropsAnUnclosedSpanAtABlankLineAndPairsTheNextLineFromScratch() {
        List<KbClassReferenceExtractor.Candidate> extracted = candidatesOf("open `unclosed", "", "`AfterBlank` here");

        assertThat(extracted).containsExactly(new KbClassReferenceExtractor.Candidate(3, "AfterBlank"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"```", "~~~", "   ```java"})
    void dropsAnUnclosedSpanAtAFenceLine(String fenceLine) {
        List<KbClassReferenceExtractor.Candidate> extracted = candidatesOf("open `unclosed", fenceLine, "plain", fenceLine.strip().substring(0, 3), "`AfterFence` here");

        assertThat(extracted).containsExactly(new KbClassReferenceExtractor.Candidate(5, "AfterFence"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"# Heading", "   ## Second level"})
    void dropsAnUnclosedSpanAtAHeadingLine(String headingLine) {
        List<KbClassReferenceExtractor.Candidate> extracted = candidatesOf("open `unclosed", headingLine, "`AfterHeading` here");

        assertThat(extracted).containsExactly(new KbClassReferenceExtractor.Candidate(3, "AfterHeading"));
    }

    @Test
    void dropsAnUnclosedSpanAtATableRow() {
        List<KbClassReferenceExtractor.Candidate> extracted = candidatesOf("open `unclosed", "| a | b |", "`AfterRow` here");

        assertThat(extracted).containsExactly(new KbClassReferenceExtractor.Candidate(3, "AfterRow"));
    }

    @Test
    void closesNoSpanAcrossATableRow() {
        List<KbClassReferenceExtractor.Candidate> extracted = candidatesOf("| cell `unclosed |", "`AfterRow` here");

        assertThat(extracted).containsExactly(new KbClassReferenceExtractor.Candidate(2, "AfterRow"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"- ", "* ", "+ ", "1. ", "12) ", "  - ", "    * "})
    void dropsAnUnclosedSpanAtTheLineThatStartsAListItem(String itemMarker) {
        List<KbClassReferenceExtractor.Candidate> extracted = candidatesOf("open `unclosed", itemMarker + "`ItemName` here");

        assertThat(extracted).containsExactly(new KbClassReferenceExtractor.Candidate(2, "ItemName"));
    }

    @Test
    void readsEachLineInsideAFencedBlockOnItsOwn() {
        List<KbClassReferenceExtractor.Candidate> extracted = candidatesOf("```", "open `a", "`PhantomInFence` here", "```", "after");

        assertThat(extracted).containsExactly(new KbClassReferenceExtractor.Candidate(3, "PhantomInFence"));
    }

    @Test
    void readsANameAfterADoubleBacktickSpanThatHoldsASingleBacktick() {
        List<String> extracted = tokensOf("Deny `cd` arguments with `..`, `` ` `` or `~`, and `PhantomAfterDouble` here.");

        assertThat(extracted).containsExactly("PhantomAfterDouble");
    }

    @Test
    void readsANameAfterADoubleBacktickSpanThatHoldsABacktickInsideText() {
        List<String> extracted = tokensOf("Wrapped ``a`b`` then `PhantomAfterInner`.");

        assertThat(extracted).containsExactly("PhantomAfterInner");
    }

    @Test
    void readsANameInsideADoubleBacktickSpan() {
        List<String> extracted = tokensOf("See ``SomeName`` now.");

        assertThat(extracted).containsExactly("SomeName");
    }

    @Test
    void readsANameInsideADoubleBacktickSpanPaddedWithSpaces() {
        List<String> extracted = tokensOf("See `` PaddedName `` now.");

        assertThat(extracted).containsExactly("PaddedName");
    }

    @Test
    void closesASpanOnlyAtARunOfTheSameLength() {
        List<String> extracted = tokensOf("`` cd `echo <missing dir>` `` | `PhantomInRow`");

        assertThat(extracted).containsExactly("PhantomInRow");
    }

    @Test
    void treatsAnUnclosedRunOfBackticksAsLiteralText() {
        List<String> extracted = tokensOf("A lone ``` then `PhantomAfterRun` here.");

        assertThat(extracted).containsExactly("PhantomAfterRun");
    }

    @Test
    void dropsAnUnclosedSpanAtAnHtmlCommentLine() {
        List<KbClassReferenceExtractor.Candidate> extracted = candidatesOf("open `unclosed", "<!-- MODULE gate -->", "`AfterComment` here");

        assertThat(extracted).containsExactly(new KbClassReferenceExtractor.Candidate(3, "AfterComment"));
    }

    @Test
    void closesASingleBacktickSpanOnlyAtALoneBacktickPastADoubleBacktickRun() {
        List<String> extracted = tokensOf("`a ``b`` c` and `PhantomAfterDoubleInside`");

        assertThat(extracted).containsExactly("PhantomAfterDoubleInside");
    }

    @Test
    void leavesADoubleBacktickRunInsideASingleBacktickSpanUnread() {
        List<String> extracted = tokensOf("`a ``SwallowedName`` c` tail");

        assertThat(extracted).isEmpty();
    }

    @Test
    void neverClosesASpanOnTheTailOfALongerRun() {
        List<String> extracted = tokensOf("`a ``b` c `PhantomTailCase`");

        assertThat(extracted).containsExactly("PhantomTailCase");
    }

    @Test
    void doesNotLetATildeLineCloseABacktickFence() {
        List<KbClassReferenceExtractor.Candidate> extracted = candidatesOf("```", "~~~", "open `unclosed", "`InsideFenceName` here", "```");

        assertThat(extracted).containsExactly(new KbClassReferenceExtractor.Candidate(4, "InsideFenceName"));
    }

    @Test
    void doesNotLetABacktickLineCloseATildeFence() {
        List<KbClassReferenceExtractor.Candidate> extracted = candidatesOf("~~~", "```", "open `unclosed", "`InsideTildeName` here", "~~~");

        assertThat(extracted).containsExactly(new KbClassReferenceExtractor.Candidate(4, "InsideTildeName"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"```", " ```", "  ```", "   ```"})
    void opensAFenceWithAnIndentOfUpToThreeSpaces(String fenceLine) {
        List<KbClassReferenceExtractor.Candidate> extracted = candidatesOf(fenceLine, "open `unclosed", "`InFence` here");

        assertThat(extracted).containsExactly(new KbClassReferenceExtractor.Candidate(3, "InFence"));
    }

    @Test
    void opensNoFenceWithAnIndentOfFourSpaces() {
        List<KbClassReferenceExtractor.Candidate> extracted = candidatesOf("    ```", "open `unclosed", "`NotInFence` here");

        assertThat(extracted).isEmpty();
    }

    @Test
    void takesAnIndentedTableRowAsABoundary() {
        List<KbClassReferenceExtractor.Candidate> extracted = candidatesOf("open `unclosed", "  | `RowName` | b |");

        assertThat(extracted).containsExactly(new KbClassReferenceExtractor.Candidate(2, "RowName"));
    }

    @Test
    void takesAnIndentedHtmlCommentLineAsABoundary() {
        List<KbClassReferenceExtractor.Candidate> extracted = candidatesOf("open `unclosed", "  <!-- MODULE `CommentName` -->");

        assertThat(extracted).containsExactly(new KbClassReferenceExtractor.Candidate(2, "CommentName"));
    }

    @Test
    void stripsNoPaddingFromASpanThatIsPaddedOnOneSideOnly() {
        List<String> extracted = tokensOf("`` LeadingOnly``");

        assertThat(extracted).isEmpty();
    }

    @Test
    void takesAWhitespaceOnlyLineAsABlankLine() {
        List<KbClassReferenceExtractor.Candidate> extracted = candidatesOf("open `unclosed", "   ", "`AfterSpaces` here");

        assertThat(extracted).containsExactly(new KbClassReferenceExtractor.Candidate(3, "AfterSpaces"));
    }

    @Test
    void readsANameInATableRowAndInAHeading() {
        List<KbClassReferenceExtractor.Candidate> extracted = candidatesOf("# About `PhantomInHeading`", "| `PhantomInCell` | b |");

        assertThat(extracted).containsExactly(
                new KbClassReferenceExtractor.Candidate(1, "PhantomInHeading"),
                new KbClassReferenceExtractor.Candidate(2, "PhantomInCell"));
    }

    @Test
    void readsNoSpanAcrossTheClosingFence() {
        List<KbClassReferenceExtractor.Candidate> extracted = candidatesOf("```", "inside `unclosed", "```", "`AfterFence` here");

        assertThat(extracted).containsExactly(new KbClassReferenceExtractor.Candidate(4, "AfterFence"));
    }

    private static List<String> tokensOf(String... lines) {
        return candidatesOf(lines).stream().map(KbClassReferenceExtractor.Candidate::token).toList();
    }

    private static List<KbClassReferenceExtractor.Candidate> candidatesOf(String... lines) {
        return KbClassReferenceExtractor.extractCandidates(List.of(lines));
    }
}
