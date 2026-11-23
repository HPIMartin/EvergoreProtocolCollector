package {{BASE_PACKAGE}}.kbguard;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.entry;

class ListedTypeNamesTest {
    private static final List<String> WELL_FORMED_BLOCK = List.of(
            "# Title",
            "- **DOC-11 Something:** text with `NotListedHere`",
            "    - `OutsideBefore`: never read",
            "- **DOC-12 A backticked type name names a real type:** rule text.",
            "  - Why: a reason with `IgnoredWhy`.",
            "  - Example list:",
            "    - `PriceCalculationAcceptanceTest`: the handbook's example.",
            "    - `TransferType`: the glossary's example,",
            "      wrapped onto a second line.",
            "  - Stop list:",
            "    - `D2AcceptanceTest`: a counter-example.",
            "    - `JavaCompile`: a Gradle API class.",
            "- **DOC-13 Next:** text",
            "  - Example list:",
            "    - `OutsideAfter`: never read");

    @Test
    void readsTheExampleListWithItsReasons() {
        ListedTypeNames tested = ListedTypeNames.parse(WELL_FORMED_BLOCK, "README.md");

        assertThat(tested.examples()).containsOnly(
                entry("PriceCalculationAcceptanceTest", "the handbook's example."),
                entry("TransferType", "the glossary's example, wrapped onto a second line."));
    }

    @Test
    void readsTheStopListWithItsReasons() {
        ListedTypeNames tested = ListedTypeNames.parse(WELL_FORMED_BLOCK, "README.md");

        assertThat(tested.stops()).containsOnly(
                entry("D2AcceptanceTest", "a counter-example."),
                entry("JavaCompile", "a Gradle API class."));
    }

    @Test
    void answersWhetherANameIsOnEitherList() {
        ListedTypeNames tested = ListedTypeNames.parse(WELL_FORMED_BLOCK, "README.md");

        List<Boolean> answers = List.of(tested.contains("TransferType"), tested.contains("JavaCompile"), tested.contains("Unlisted"));

        assertThat(answers).containsExactly(true, true, false);
    }

    @Test
    void neverReadsAnEntryOutsideTheDoc12Block() {
        ListedTypeNames tested = ListedTypeNames.parse(WELL_FORMED_BLOCK, "README.md");

        List<Boolean> answers = List.of(tested.contains("OutsideBefore"), tested.contains("OutsideAfter"));

        assertThat(answers).containsExactly(false, false);
    }

    @Test
    void readsNoNameFromASubBulletInsideTheBlockThatIsNoListEntry() {
        ListedTypeNames tested = ListedTypeNames.parse(WELL_FORMED_BLOCK, "README.md");

        boolean listed = tested.contains("IgnoredWhy");

        assertThat(listed).isFalse();
    }

    @Test
    void failsNamingTheLineOfAnEntryWhoseNameTheCandidateRuleDoesNotYield() {
        List<String> lines = List.of(
                "- **DOC-12 Rule:** text",
                "  - Example list:",
                "    - `Date.now()`: a dotted access, whose candidate is Date.",
                "  - Stop list:");

        Throwable failure = catchThrowable(() -> ListedTypeNames.parse(lines, "README.md"));

        assertThat(failure).isInstanceOf(IllegalStateException.class).hasMessageContaining("README.md:3").hasMessageContaining("Date.now()");
    }

    @Test
    void readsAOneHumpEntryTheCandidateRuleYieldsHeadingADottedAccess() {
        List<String> lines = List.of(
                "- **DOC-12 Rule:** text",
                "  - Example list:",
                "  - Stop list:",
                "    - `Browsers`: one hump, a candidate only heading `Browsers.quitAll`.");

        ListedTypeNames tested = ListedTypeNames.parse(lines, "README.md");

        assertThat(tested.stops()).containsOnly(entry("Browsers", "one hump, a candidate only heading `Browsers.quitAll`."));
    }

    @Test
    void readsAOneHumpExampleEntryTheCandidateRuleYieldsHeadingADottedAccess() {
        List<String> lines = List.of(
                "- **DOC-12 Rule:** text",
                "  - Example list:",
                "    - `Browsers`: one hump, a candidate only heading `Browsers.quitAll`.",
                "  - Stop list:");

        ListedTypeNames tested = ListedTypeNames.parse(lines, "README.md");

        assertThat(tested.examples()).containsOnly(entry("Browsers", "one hump, a candidate only heading `Browsers.quitAll`."));
    }

    @Test
    void failsOnASingleWordEntryWithoutALowercaseLetter() {
        List<String> lines = List.of(
                "- **DOC-12 Rule:** text",
                "  - Example list:",
                "  - Stop list:",
                "    - `URL`: all capitals, never a candidate.");

        Throwable failure = catchThrowable(() -> ListedTypeNames.parse(lines, "README.md"));

        assertThat(failure).isInstanceOf(IllegalStateException.class).hasMessageContaining("README.md:4").hasMessageContaining("URL");
    }

    @Test
    void endsTheBlockAtAHeadingLine() {
        List<String> lines = List.of(
                "- **DOC-12 Rule:** text",
                "  - Example list:",
                "    - `InsideName`: inside.",
                "  - Stop list:",
                "## Heading",
                "    - `AfterHeading`: outside.");

        ListedTypeNames tested = ListedTypeNames.parse(lines, "README.md");

        assertThat(tested.contains("AfterHeading")).isFalse();
        assertThat(tested.contains("InsideName")).isTrue();
    }

    @Test
    void endsTheBlockAtTheEndOfTheFile() {
        List<String> lines = List.of(
                "- **DOC-12 Rule:** text",
                "  - Example list:",
                "    - `LastExample`: at the end of the file.",
                "  - Stop list:",
                "    - `LastStop`: also at the end.");

        ListedTypeNames tested = ListedTypeNames.parse(lines, "README.md");

        assertThat(tested.examples()).containsOnlyKeys("LastExample");
        assertThat(tested.stops()).containsOnlyKeys("LastStop");
    }

    @Test
    void endsTheCurrentListAtAnyOtherSubBullet() {
        List<String> lines = List.of(
                "- **DOC-12 Rule:** text",
                "  - Example list:",
                "    - `ListedName`: listed.",
                "  - Another sub-bullet:",
                "    - `NotAnEntry`: belongs to no list.",
                "  - Stop list:");

        ListedTypeNames tested = ListedTypeNames.parse(lines, "README.md");

        assertThat(tested.contains("NotAnEntry")).isFalse();
        assertThat(tested.contains("ListedName")).isTrue();
    }

    @Test
    void failsNamingTheLineOfAnEntryWithoutAReason() {
        List<String> lines = List.of(
                "- **DOC-12 Rule:** text",
                "  - Example list:",
                "    - `FirstName`: has a reason.",
                "    - `SecondName`",
                "  - Stop list:");

        Throwable failure = catchThrowable(() -> ListedTypeNames.parse(lines, "README.md"));

        assertThat(failure).isInstanceOf(IllegalStateException.class).hasMessageContaining("README.md:4").hasMessageContaining("SecondName");
    }

    @Test
    void failsWhenTheReadmeHasNoDoc12Block() {
        List<String> lines = List.of("- **DOC-11 Rule:** text", "  - Example list:", "  - Stop list:");

        Throwable failure = catchThrowable(() -> ListedTypeNames.parse(lines, "README.md"));

        assertThat(failure).isInstanceOf(IllegalStateException.class).hasMessageContaining("README.md").hasMessageContaining("no DOC-12 block");
    }

    @Test
    void endsTheBlockAtAParagraphLineAtColumnZero() {
        List<String> lines = List.of(
                "- **DOC-12 Rule:** text",
                "  - Example list:",
                "  - Stop list:",
                "    - `StopName`: listed.",
                "A paragraph at column zero.",
                "    - `AfterParagraph`: outside the block.");

        ListedTypeNames tested = ListedTypeNames.parse(lines, "README.md");

        assertThat(tested.contains("AfterParagraph")).isFalse();
        assertThat(tested.contains("StopName")).isTrue();
    }

    @Test
    void readsAHeaderWithTrailingWhitespace() {
        List<String> lines = List.of(
                "- **DOC-12 Rule:** text",
                "  - Example list: ",
                "    - `ExampleName`: listed.",
                "  - Stop list:\t",
                "    - `StopName`: listed.");

        ListedTypeNames tested = ListedTypeNames.parse(lines, "README.md");

        assertThat(tested.examples()).containsOnlyKeys("ExampleName");
        assertThat(tested.stops()).containsOnlyKeys("StopName");
    }

    @Test
    void keepsTheBlockGoingAcrossABlankLine() {
        List<String> lines = List.of(
                "- **DOC-12 R:** t",
                "  - Example list:",
                "",
                "    - `AfterBlank`: r.",
                "  - Stop list:");

        ListedTypeNames tested = ListedTypeNames.parse(lines, "README.md");

        assertThat(tested.examples()).containsOnlyKeys("AfterBlank");
    }

    @Test
    void namesTheLineThatEndedTheBlockBeforeAHeader() {
        List<String> lines = List.of(
                "- **DOC-12 R:** t",
                "  - Example list:",
                "<!-- MODULE x -->",
                "    - `AfterComment`: r.",
                "<!-- /MODULE x -->",
                "  - Stop list:");

        Throwable failure = catchThrowable(() -> ListedTypeNames.parse(lines, "README.md"));

        assertThat(failure).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ends at README.md:3")
                .hasMessageContaining("<!-- MODULE x -->")
                .hasMessageContaining("before its 'Stop list:' header");
    }

    @Test
    void failsOnAnEntryWhoseReasonIsBlank() {
        List<String> lines = List.of("- **DOC-12 Rule:** text", "  - Example list:", "    - `BlankReason`:   ", "  - Stop list:");

        Throwable failure = catchThrowable(() -> ListedTypeNames.parse(lines, "README.md"));

        assertThat(failure).isInstanceOf(IllegalStateException.class).hasMessageContaining("README.md:3").hasMessageContaining("BlankReason");
    }

    @Test
    void failsWhenTheBlockHasNoExampleListHeader() {
        List<String> lines = List.of("- **DOC-12 Rule:** text", "  - Stop list:", "    - `StopName`: reason.");

        Throwable failure = catchThrowable(() -> ListedTypeNames.parse(lines, "README.md"));

        assertThat(failure).isInstanceOf(IllegalStateException.class).hasMessageContaining("Example list");
    }

    @Test
    void failsWhenTheBlockHasNoStopListHeader() {
        List<String> lines = List.of("- **DOC-12 Rule:** text", "  - Example list:", "    - `ExampleName`: reason.");

        Throwable failure = catchThrowable(() -> ListedTypeNames.parse(lines, "README.md"));

        assertThat(failure).isInstanceOf(IllegalStateException.class).hasMessageContaining("Stop list");
    }
}
