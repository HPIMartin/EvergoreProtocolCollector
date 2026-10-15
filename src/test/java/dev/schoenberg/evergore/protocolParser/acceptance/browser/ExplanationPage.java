package dev.schoenberg.evergore.protocolParser.acceptance.browser;

import java.util.List;

public record ExplanationPage(String text, List<String> legend, boolean legendAboveTheGroups, List<Group> groups) {
	public record Group(String title, int titleLevel, List<Answer> answers) {}

	public record Answer(String question, int questionLevel, String example, int examples, boolean ruleBeforeExample) {}

	public List<Answer> answers() {
		return groups.stream().flatMap(group -> group.answers().stream()).toList();
	}
}
