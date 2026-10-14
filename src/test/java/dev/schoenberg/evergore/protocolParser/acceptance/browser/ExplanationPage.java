package dev.schoenberg.evergore.protocolParser.acceptance.browser;

import java.util.List;

public record ExplanationPage(String heading, String text, List<String> legend, boolean legendAboveTheGroups, List<Group> groups) {
	public record Group(String title, int titleLevel, List<Answer> answers) {}

	public record Answer(String question, int questionLevel, String rule, List<String> points, String example, String exampleIntro, List<String> calculations, int examples,
			boolean ruleBeforeExample) {}

	public List<Answer> answers() {
		return groups.stream().flatMap(group -> group.answers().stream()).toList();
	}

	public Answer answerTo(String question) {
		return answers().stream().filter(answer -> answer.question().equals(question)).findFirst().orElseThrow(() -> new AssertionError("The page does not ask: " + question));
	}
}
