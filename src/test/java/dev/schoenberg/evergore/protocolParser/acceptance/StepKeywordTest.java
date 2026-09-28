package dev.schoenberg.evergore.protocolParser.acceptance;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.core.cli.Main;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StepKeywordTest {
	private static final Path MESSAGES = Paths.get("build/tmp/stepKeywords/dry-run.ndjson");
	private static final ObjectMapper JSON = new ObjectMapper();

	@Test
	void everyStepDefinitionStandsUnderOneKeywordAcrossTheSuite() throws IOException {
		List<JsonNode> envelopes = dryRunOfEveryScenario();

		Map<String, Set<String>> keywordsPerDefinition = keywordsPerDefinition(envelopes);

		Map<String, Set<String>> underSeveralKeywords = new TreeMap<>(keywordsPerDefinition);
		underSeveralKeywords.values().removeIf(keywords -> keywords.size() == 1);
		assertThat(keywordsPerDefinition).isNotEmpty();
		assertThat(underSeveralKeywords).isEmpty();
	}

	private static List<JsonNode> dryRunOfEveryScenario() throws IOException {
		Files.createDirectories(MESSAGES.getParent());
		Main
				.run(new String[]{"--dry-run", "--tags", "@wip or not @wip", "--glue", StepKeywordTest.class.getPackageName(), "--plugin", "message:" + MESSAGES,
						"classpath:features"}, StepKeywordTest.class.getClassLoader());
		try (var lines = Files.lines(MESSAGES)) {
			return lines.map(StepKeywordTest::envelopeOf).toList();
		}
	}

	private static Map<String, Set<String>> keywordsPerDefinition(List<JsonNode> envelopes) {
		Map<String, String> patterns = new TreeMap<>();
		Map<String, String> keywordOfPickleStep = new TreeMap<>();
		envelopes
				.stream()
				.filter(envelope -> envelope.has("stepDefinition"))
				.map(envelope -> envelope.get("stepDefinition"))
				.forEach(definition -> patterns.put(definition.get("id").asText(), definition.get("pattern").get("source").asText()));
		envelopes
				.stream()
				.filter(envelope -> envelope.has("pickle"))
				.flatMap(envelope -> envelope.get("pickle").get("steps").valueStream())
				.forEach(step -> keywordOfPickleStep.put(step.get("id").asText(), step.get("type").asText()));

		Map<String, Set<String>> keywords = new TreeMap<>();
		envelopes
				.stream()
				.filter(envelope -> envelope.has("testCase"))
				.flatMap(envelope -> envelope.get("testCase").get("testSteps").valueStream())
				.filter(step -> step.has("pickleStepId"))
				.forEach(step -> step
						.get("stepDefinitionIds")
						.valueStream()
						.forEach(id -> keywords
								.computeIfAbsent(patterns.get(id.asText()), pattern -> new TreeSet<>())
								.add(keywordOfPickleStep.get(step.get("pickleStepId").asText()))));
		return keywords;
	}

	private static JsonNode envelopeOf(String line) {
		try {
			return JSON.readTree(line);
		} catch (IOException e) {
			throw new IllegalStateException("The dry run wrote a line that is no message: " + line, e);
		}
	}
}
