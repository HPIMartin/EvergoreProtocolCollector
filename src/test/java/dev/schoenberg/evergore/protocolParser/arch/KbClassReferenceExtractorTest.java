package dev.schoenberg.evergore.protocolParser.arch;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class KbClassReferenceExtractorTest {

	@Test
	void extractsACompoundPascalCaseNameFromBackticks() {
		List<String> extracted = KbClassReferenceExtractor.extractCandidates("See `EvergoreDataEvaluator` for details.");

		assertThat(extracted).containsExactly("EvergoreDataEvaluator");
	}

	@ParameterizedTest
	@CsvSource({"`Configuration.browser`,Configuration", "`Browser.DOCKER`,Browser"})
	void extractsOnlyTheLeadingSegmentOfADottedMemberAccess(String backtickSpan, String expectedLeadingSegment) {
		List<String> extracted = KbClassReferenceExtractor.extractCandidates(backtickSpan);

		assertThat(extracted).containsExactly(expectedLeadingSegment);
	}

	@ParameterizedTest
	@ValueSource(strings = {"`CRLF`", "`D5`"})
	void excludesATokenWithNoLowercaseLetter(String backtickSpan) {
		List<String> extracted = KbClassReferenceExtractor.extractCandidates(backtickSpan);

		assertThat(extracted).isEmpty();
	}

	@ParameterizedTest
	@ValueSource(strings = {"`Feonir`", "`Einzahlung`", "`Agent`"})
	void excludesASingleWordTokenWithFewerThanTwoCapitalizedHumps(String backtickSpan) {
		List<String> extracted = KbClassReferenceExtractor.extractCandidates(backtickSpan);

		assertThat(extracted).isEmpty();
	}

	@Test
	void excludesArchUnitAsAProductNameWithNoLiteralClass() {
		List<String> extracted = KbClassReferenceExtractor.extractCandidates("`ArchUnit` guards the hexagonal boundary.");

		assertThat(extracted).isEmpty();
	}

	@Test
	void excludesNeedBracesAsACheckstyleRuleShortNameNotOnTheApplicationClasspath() {
		List<String> extracted = KbClassReferenceExtractor.extractCandidates("The `NeedBraces` rule is active.");

		assertThat(extracted).isEmpty();
	}

	@Test
	void excludesAvoidStarImportAsACheckstyleRuleShortNameNotOnTheApplicationClasspath() {
		List<String> extracted = KbClassReferenceExtractor.extractCandidates("The `AvoidStarImport` rule is active.");

		assertThat(extracted).isEmpty();
	}

	@Test
	void excludesJavaCompileAsAGradleApiClassOnlyOnTheBuildScriptClasspath() {
		List<String> extracted = KbClassReferenceExtractor.extractCandidates("Configured via `JavaCompile`.");

		assertThat(extracted).isEmpty();
	}

	@Test
	void excludesEnterWorktreeAsAClaudeCodeToolNameNotAProjectClass() {
		List<String> extracted = KbClassReferenceExtractor.extractCandidates("Run `EnterWorktree` first.");

		assertThat(extracted).isEmpty();
	}

	@Test
	void excludesD2AcceptanceTestAsTheHandbooksOwnCounterExampleOfABadClassName() {
		List<String> extracted = KbClassReferenceExtractor.extractCandidates("Not `D2AcceptanceTest`.");

		assertThat(extracted).isEmpty();
	}

	@Test
	void excludesSonarSourceAsAVsCodeExtensionPublisherIdNotAClass() {
		List<String> extracted = KbClassReferenceExtractor.extractCandidates("The `SonarSource.sonarlint-vscode` extension is declared directly.");

		assertThat(extracted).isEmpty();
	}

	@Test
	void excludesStrictHostKeyCheckingAsAnOpenSshClientOptionNotAClass() {
		List<String> extracted = KbClassReferenceExtractor.extractCandidates("The deploy runs with `StrictHostKeyChecking=yes`.");

		assertThat(extracted).isEmpty();
	}

	@Test
	void extractsAMultiHumpTokenThatIsNotStoplisted() {
		List<String> extracted = KbClassReferenceExtractor.extractCandidates("`TokenScopeTest` proves the default-deny scope.");

		assertThat(extracted).containsExactly("TokenScopeTest");
	}
}
