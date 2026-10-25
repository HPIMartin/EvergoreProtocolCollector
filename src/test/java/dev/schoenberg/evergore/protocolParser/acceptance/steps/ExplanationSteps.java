package dev.schoenberg.evergore.protocolParser.acceptance.steps;

import java.util.List;
import java.util.regex.Pattern;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import dev.schoenberg.evergore.protocolParser.acceptance.browser.ExplanationPage;
import dev.schoenberg.evergore.protocolParser.acceptance.browser.ExplanationPage.Answer;
import dev.schoenberg.evergore.protocolParser.acceptance.browser.MemberBrowser;

import static dev.schoenberg.evergore.protocolParser.acceptance.steps.Pages.EXPLANATION;
import static org.assertj.core.api.Assertions.assertThat;

public class ExplanationSteps {
	private static final Pattern SENTENCE_END = Pattern.compile("(?<=[.!?])\\s+");

	private final MemberBrowser browser;

	public ExplanationSteps(MemberBrowser browser) {
		this.browser = browser;
	}

	@When("a member opens the explanation page")
	public void aMemberOpensTheExplanationPage() {
		browser.opening().open(EXPLANATION);
	}

	@When("the member follows {string}")
	public void theMemberFollows(String linkText) {
		browser.follow(linkText);
	}

	@Then("the page asks exactly these questions, in this order, under these groups:")
	public void thePageAsksExactlyTheseQuestions(DataTable expected) {
		List<List<String>> asked = page().groups().stream().flatMap(group -> group.answers().stream().map(answer -> List.of(group.title(), answer.question()))).toList();

		assertThat(asked).isEqualTo(expected.cells().subList(1, expected.height()));
	}

	@Then("each group's title is a heading of the page, and each of its questions a heading one level below it")
	public void eachGroupTitleIsAHeadingAboveItsQuestions() {
		assertThat(page().groups()).isNotEmpty().allSatisfy(group -> {
			assertThat(group.titleLevel()).as("the heading level of %s", group.title()).isPositive();
			assertThat(group.answers()).allSatisfy(answer -> assertThat(answer.questionLevel()).as("the heading level of %s", answer.question()).isEqualTo(group.titleLevel() + 1));
		});
	}

	@Then("the page says, above its first group:")
	public void thePageSaysAboveItsFirstGroup(DataTable expected) {
		ExplanationPage page = page();

		List<String> sentences = page.legend().stream().flatMap(SENTENCE_END::splitAsStream).toList();

		assertThat(page.legendAboveTheGroups()).as("the legend stands above the first group").isTrue();
		for (List<String> row : expected.cells().subList(1, expected.height())) {
			assertThat(sentences).as("a sentence naming %s as %s", row.get(0), row.get(1)).anyMatch(sentence -> sentence.contains(row.get(0)) && sentence.contains(row.get(1)));
		}
	}

	@Then("each of the {int} questions on the page has an answer whose rule stands before one example that opens with {string} and names {string}, {string} or {string}")
	public void eachQuestionHasItsRuleBeforeOneExample(int count, String opener, String first, String second, String third) {
		List<Answer> answers = page().answers();

		assertThat(answers).hasSize(count);
		assertThat(answers).allSatisfy(answer -> {
			assertThat(answer.ruleBeforeExample()).as("the rule of %s stands before its example", answer.question()).isTrue();
			assertThat(answer.examples()).as("the examples of %s", answer.question()).isEqualTo(1);
			assertThat(answer.example()).startsWith(opener).containsAnyOf(first, second, third);
		});
	}

	@Then("the page does not say {string}")
	public void thePageDoesNotSay(String words) {
		assertThat(page().text()).doesNotContain(words);
	}

	private ExplanationPage page() {
		return browser.read("read-explanation.js", ExplanationPage.class);
	}
}
