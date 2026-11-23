package {{BASE_PACKAGE}}.kbguard;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class KbCitationGuardTest {
    private static final Path KNOWLEDGE_BASE_DIRECTORY = Path.of("docs/knowledge-base");
    private static final String FRONTEND_SOURCE_ROOT = "";
    private static final String README_WITH_EMPTY_LISTS = "- **DOC-12 Rule:** text\n  - Example list:\n  - Stop list:\n";

    @TempDir
    Path tempDirectory;

    @Test
    void flagsAnUnresolvedClassReferenceAsFileColonLineColonToken() throws IOException {
        Files.writeString(tempDirectory.resolve("README.md"), README_WITH_EMPTY_LISTS);
        Files.writeString(tempDirectory.resolve("phantom.md"), "Uses `DatabaseStartupInitialization` at startup.\n");

        List<String> unresolved = unresolvedReferencesUnder(tempDirectory, tempDirectory, "");

        assertThat(unresolved).containsExactly("phantom.md:1: DatabaseStartupInitialization");
    }

    @Test
    void flagsANameAfterACodeSpanThatWrapsOntoTheLine() throws IOException {
        Files.writeString(tempDirectory.resolve("README.md"), README_WITH_EMPTY_LISTS);
        Files.writeString(tempDirectory.resolve("page.md"),
                "Line one opens a span `git merge --ff-only\nrefs/remotes/origin/main` and then cites `PhantomAfterWrappedSpan` here.\n");

        List<String> unresolved = unresolvedReferencesUnder(tempDirectory, tempDirectory, "");

        assertThat(unresolved).containsExactly("page.md:2: PhantomAfterWrappedSpan");
    }

    @Test
    void flagsANameThatOpensAWrappingSpanAtTheLineWhereTheSpanOpens() throws IOException {
        Files.writeString(tempDirectory.resolve("README.md"), README_WITH_EMPTY_LISTS);
        Files.writeString(tempDirectory.resolve("page.md"), "first line\nSee `PhantomWrappedOpener\ncontinues here` done.\n");

        List<String> unresolved = unresolvedReferencesUnder(tempDirectory, tempDirectory, "");

        assertThat(unresolved).containsExactly("page.md:2: PhantomWrappedOpener");
    }

    @Test
    void readsALineInsideAFencedBlockOnItsOwn() throws IOException {
        Files.writeString(tempDirectory.resolve("README.md"), README_WITH_EMPTY_LISTS);
        Files.writeString(tempDirectory.resolve("page.md"), "text\n```\nopen `unclosed\n`PhantomInFence` here\n```\n");

        List<String> unresolved = unresolvedReferencesUnder(tempDirectory, tempDirectory, "");

        assertThat(unresolved).containsExactly("page.md:4: PhantomInFence");
    }

    @Test
    void readsAPageBelowTheKnowledgeBaseDirectoryAndNamesItByItsRelativePath() throws IOException {
        Files.writeString(tempDirectory.resolve("README.md"), README_WITH_EMPTY_LISTS);
        Files.createDirectories(tempDirectory.resolve("sub/deeper"));
        Files.writeString(tempDirectory.resolve("sub/deeper/inner.md"), "`GhostInside` is cited.\n");

        List<String> unresolved = unresolvedReferencesUnder(tempDirectory, tempDirectory, "");

        assertThat(unresolved).containsExactly("sub/deeper/inner.md:1: GhostInside");
    }

    @Test
    void namesAPageThatIsNotValidUtf8InItsFailure() throws IOException {
        Files.writeString(tempDirectory.resolve("README.md"), README_WITH_EMPTY_LISTS);
        Files.write(tempDirectory.resolve("latin1.md"), "café `Name`\n".getBytes(StandardCharsets.ISO_8859_1));

        Throwable failure = catchThrowable(() -> unresolvedReferencesUnder(tempDirectory, tempDirectory, ""));

        assertThat(failure).isInstanceOf(UncheckedIOException.class).hasMessageContaining("latin1.md");
    }

    @Test
    void doesNotFlagAJdkClassName() throws IOException {
        Files.writeString(tempDirectory.resolve("README.md"), README_WITH_EMPTY_LISTS);
        Files.writeString(tempDirectory.resolve("page.md"), "`ArrayList` is a JDK class.\n");

        List<String> unresolved = unresolvedReferencesUnder(tempDirectory, tempDirectory, "");

        assertThat(unresolved).isEmpty();
    }

    @Test
    void doesNotFlagANameOfEitherListOrOneTheFrontendDeclares() throws IOException {
        Path frontendRoot = Files.createDirectories(tempDirectory.resolve("frontend"));
        Files.writeString(frontendRoot.resolve("widget.ts"), "export class FrontendOnlyWidget {}\n");
        Path knowledgeBase = Files.createDirectories(tempDirectory.resolve("kb"));
        Files.writeString(knowledgeBase.resolve("README.md"),
                "- **DOC-12 Rule:** text\n  - Example list:\n    - `ExampleOnlyName`: an example.\n  - Stop list:\n    - `StopOnlyName`: not a type.\n");
        Files.writeString(knowledgeBase.resolve("page.md"), "`ExampleOnlyName`, `StopOnlyName` and `FrontendOnlyWidget` are named.\n");

        List<String> unresolved = unresolvedReferencesUnder(tempDirectory, knowledgeBase, "frontend");

        assertThat(unresolved).isEmpty();
    }

    @Test
    void flagsANameThatNeitherTheFrontendNorEitherListHolds() throws IOException {
        Path frontendRoot = Files.createDirectories(tempDirectory.resolve("frontend"));
        Files.writeString(frontendRoot.resolve("widget.ts"), "export class FrontendOnlyWidget {}\n");
        Path knowledgeBase = Files.createDirectories(tempDirectory.resolve("kb"));
        Files.writeString(knowledgeBase.resolve("README.md"), README_WITH_EMPTY_LISTS);
        Files.writeString(knowledgeBase.resolve("page.md"), "first line\n`FrontendOnlyWidget` and `GhostWidget`.\n");

        List<String> unresolved = unresolvedReferencesUnder(tempDirectory, knowledgeBase, "frontend");

        assertThat(unresolved).containsExactly("page.md:2: GhostWidget");
    }

    @Test
    void theKnowledgeBaseCarriesNoUnresolvedTypeNameReference() {
        List<String> unresolved = unresolvedReferencesUnder(Path.of(""), KNOWLEDGE_BASE_DIRECTORY, FRONTEND_SOURCE_ROOT);

        assertThat(unresolved).isEmpty();
    }

    private static List<String> unresolvedReferencesUnder(Path projectDirectory, Path knowledgeBase, String frontendSourceRoot) {
        KnownJavaSymbols javaSymbols = KnownJavaSymbols.fromTestRuntimeClasspath();
        KnownFrontendSymbols frontendSymbols = KnownFrontendSymbols.fromSourceRoot(projectDirectory, frontendSourceRoot);
        ListedTypeNames listedNames = readListedNames(knowledgeBase.resolve("README.md"));
        List<String> unresolved = new ArrayList<>();
        try (Stream<Path> markdownFiles = Files.walk(knowledgeBase)) {
            List<Path> sortedMarkdownFiles = markdownFiles.filter(path -> path.toString().endsWith(".md")).sorted().toList();
            for (Path markdownFile : sortedMarkdownFiles) {
                String pageName = knowledgeBase.relativize(markdownFile).toString().replace('\\', '/');
                unresolved.addAll(unresolvedReferencesIn(markdownFile, pageName, javaSymbols, frontendSymbols, listedNames));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return unresolved;
    }

    private static ListedTypeNames readListedNames(Path readme) {
        return ListedTypeNames.parse(readLines(readme), readme.toString());
    }

    private static List<String> readLines(Path file) {
        try {
            return Files.readAllLines(file);
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read " + file, e);
        }
    }

    private static List<String> unresolvedReferencesIn(Path markdownFile, String pageName, KnownJavaSymbols javaSymbols, KnownFrontendSymbols frontendSymbols, ListedTypeNames listedNames) {
        List<String> unresolved = new ArrayList<>();
        for (KbClassReferenceExtractor.Candidate candidate : KbClassReferenceExtractor.extractCandidates(readLines(markdownFile))) {
            String name = candidate.token();
            boolean resolved = javaSymbols.hasSimpleName(name) || frontendSymbols.hasName(name) || listedNames.contains(name);
            if (!resolved) {
                unresolved.add(pageName + ":" + candidate.lineNumber() + ": " + name);
            }
        }
        return unresolved;
    }
}
