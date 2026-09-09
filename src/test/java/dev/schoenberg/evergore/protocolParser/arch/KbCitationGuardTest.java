package dev.schoenberg.evergore.protocolParser.arch;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

class KbCitationGuardTest {

	private static final Path KNOWLEDGE_BASE_DIRECTORY = Path.of("docs/knowledge-base");

	// file + token, not file + line: frontend.md's own prose reflows independently of this guard, so
	// pinning a line number here would fail on every incidental frontend.md edit rather than only on
	// a genuinely new reference.
	private static final Pattern LINE_NUMBER = Pattern.compile("^(.*):\\d+: (.*)$");

	private static final List<String> DISCLOSED_FRONTEND_TYPESCRIPT_REFERENCES_OUTSIDE_JAVA_SRC_SCOPE = disclosedFrontendTypescriptReferences();

	private static List<String> disclosedFrontendTypescriptReferences() {
		List<String> references = new ArrayList<>();
		references.add("frontend.md: ProtocolApi");
		references.add("frontend.md: App");
		references.add("frontend.md: PageFrame");
		references.add("frontend.md: PageFrame");
		references.add("frontend.md: SortableTable");
		references.add("frontend.md: StatusPanel");
		references.add("frontend.md: Intl");
		references.add("frontend.md: AdminView");
		references.add("frontend.md: LoadedView");
		references.add("frontend.md: SortableTable");
		references.add("frontend.md: SortableTable");
		references.add("frontend.md: SortableTable");
		references.add("frontend.md: PageFrame");
		return Collections.unmodifiableList(references);
	}

	@Test
	void flagsAnUnresolvedJavaSrcClassReferenceAsFileColonLineColonToken(@TempDir Path tempDirectory) throws IOException {
		Path phantomDoc = tempDirectory.resolve("phantom.md");
		Files.writeString(phantomDoc, "Uses `DatabaseStartupInitialization` at startup.\n");

		List<String> unresolvedJavaSrcReferences = unresolvedJavaSrcClassReferencesUnder(tempDirectory);

		assertThat(unresolvedJavaSrcReferences).containsExactly("phantom.md:1: DatabaseStartupInitialization");
	}

	@Test
	void everyDisclosedGapEntryIsRootedInFrontendMdNotAJavaSrcDoc() {
		assertThat(DISCLOSED_FRONTEND_TYPESCRIPT_REFERENCES_OUTSIDE_JAVA_SRC_SCOPE).allMatch(entry -> entry.startsWith("frontend.md:"));
	}

	@Test
	void theKnowledgeBaseCarriesNoUnresolvedJavaSrcClassReferenceBeyondTheDisclosedFrontendGap() {
		List<String> unresolvedJavaSrcReferencesWithoutLineNumbers = unresolvedJavaSrcClassReferencesUnder(KNOWLEDGE_BASE_DIRECTORY)
				.stream()
				.map(KbCitationGuardTest::withoutLineNumber)
				.toList();

		assertThat(unresolvedJavaSrcReferencesWithoutLineNumbers).containsExactlyInAnyOrderElementsOf(DISCLOSED_FRONTEND_TYPESCRIPT_REFERENCES_OUTSIDE_JAVA_SRC_SCOPE);
	}

	private static String withoutLineNumber(String reference) {
		return LINE_NUMBER.matcher(reference).replaceFirst("$1: $2");
	}

	private static List<String> unresolvedJavaSrcClassReferencesUnder(Path directory) {
		KnownJavaSymbols knownJavaSymbols = KnownJavaSymbols.fromTestRuntimeClasspath();
		List<String> unresolved = new ArrayList<>();
		try (Stream<Path> markdownFiles = Files.list(directory)) {
			List<Path> sortedMarkdownFiles = markdownFiles.filter(path -> path.toString().endsWith(".md")).sorted().toList();
			for (Path markdownFile : sortedMarkdownFiles) {
				unresolved.addAll(unresolvedReferencesIn(markdownFile, knownJavaSymbols));
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return unresolved;
	}

	private static List<String> unresolvedReferencesIn(Path markdownFile, KnownJavaSymbols knownJavaSymbols) {
		List<String> unresolved = new ArrayList<>();
		List<String> lines;
		try {
			lines = Files.readAllLines(markdownFile);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
			int lineNumber = lineIndex + 1;
			for (String candidate : KbClassReferenceExtractor.extractCandidates(lines.get(lineIndex))) {
				if (!knownJavaSymbols.hasSimpleName(candidate)) {
					unresolved.add(markdownFile.getFileName() + ":" + lineNumber + ": " + candidate);
				}
			}
		}
		return unresolved;
	}
}
