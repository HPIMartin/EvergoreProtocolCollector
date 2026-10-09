package dev.schoenberg.evergore.protocolParser.rest.netty;

import java.util.List;

import org.junit.jupiter.api.Test;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static org.assertj.core.api.Assertions.assertThat;

class HeaderRescueTest {
	private static final int ROOMY = 1024;
	private static final RescuedHeader USER_AGENT = new RescuedHeader("User-Agent", "Firefox/128.0");
	private static final RescuedHeader ACCEPTANCE_CLIENT = new RescuedHeader("X-Acceptance-Client-Address", "203.0.113.7");

	@Test
	void collectsTheHeaderLinesThatFollowTheRejectedLineUntilTheBlankLine() {
		HeaderRescue tested = HeaderRescue.after(SliceEnd.END_OF_LINE, ROOMY);

		feed(tested, "User-Agent: Firefox/128.0\r\n", "X-Acceptance-Client-Address: 203.0.113.7\r\n", "\r\n");

		assertThat(tested.isComplete()).isTrue();
		assertThat(tested.headers()).containsExactly(USER_AGENT, ACCEPTANCE_CLIENT);
	}

	@Test
	void readsTheLineTheRejectionHappenedOnAsAHeaderLineToo() {
		HeaderRescue tested = HeaderRescue.after(SliceEnd.END_OF_LINE, ROOMY);

		tested.alsoRead("X-Acceptance-Client-Address: 203.0.113.7\r\n".getBytes(ISO_8859_1));
		feed(tested, "User-Agent: Firefox/128.0\r\n", "\r\n");

		assertThat(tested.headers()).containsExactly(ACCEPTANCE_CLIENT, USER_AGENT);
	}

	@Test
	void doesNotReadAnEmptyRestOfALineAsTheBlankLineEndingTheHead() {
		HeaderRescue tested = HeaderRescue.after(SliceEnd.END_OF_LINE, ROOMY);

		tested.alsoRead("\n".getBytes(ISO_8859_1));

		assertThat(tested.isComplete()).isFalse();
	}

	@Test
	void isNotCompleteWhileTheBlankLineIsMissing() {
		HeaderRescue tested = HeaderRescue.after(SliceEnd.END_OF_LINE, ROOMY);

		feed(tested, "User-Agent: Firefox/128.0\r\n");

		assertThat(tested.isComplete()).isFalse();
	}

	@Test
	void skipsTheRestOfARejectedLineThatHasNotEndedYet() {
		HeaderRescue tested = HeaderRescue.after(SliceEnd.MID_LINE, ROOMY);

		feed(tested, "/aaaaaaaa", "aaaa:aaaa\r\n", "User-Agent: Firefox/128.0\r\n", "\r\n");

		assertThat(tested.headers()).containsExactly(USER_AGENT);
		assertThat(tested.isComplete()).isTrue();
	}

	@Test
	void readsTheRejectedLineAsAHeaderLineWhenItKnowsTheStartOfItAndGetsTheRest() {
		HeaderRescue tested = HeaderRescue.after(SliceEnd.MID_LINE, ROOMY);

		tested.finishTheRejectedLine("User-Ag".getBytes(ISO_8859_1));
		feed(tested, "ent: Firefox/128.0\r\n", "\r\n");

		assertThat(tested.headers()).containsExactly(USER_AGENT);
		assertThat(tested.isComplete()).isTrue();
	}

	@Test
	void keepsSkippingTheRejectedLineWhenItKnowsNothingOfItsStart() {
		HeaderRescue tested = HeaderRescue.after(SliceEnd.MID_LINE, ROOMY);

		tested.finishTheRejectedLine(new byte[0]);
		feed(tested, "rest: of a line that was too long\r\n", "User-Agent: Firefox/128.0\r\n", "\r\n");

		assertThat(tested.headers()).containsExactly(USER_AGENT);
	}

	@Test
	void countsTheStartOfTheRejectedLineAgainstTheBudget() {
		HeaderRescue tested = HeaderRescue.after(SliceEnd.MID_LINE, 40);

		tested.finishTheRejectedLine("a".repeat(30).getBytes(ISO_8859_1));
		feed(tested, "a".repeat(11));

		assertThat(tested.isComplete()).isTrue();
	}

	@Test
	void doesNotReadTheEndOfARejectedLineAsABlankLine() {
		HeaderRescue tested = HeaderRescue.after(SliceEnd.MID_LINE, ROOMY);

		feed(tested, "the rejected line so far\r", "\n");

		assertThat(tested.isComplete()).isFalse();
	}

	@Test
	void joinsAHeaderLineSplitAcrossSlices() {
		HeaderRescue tested = HeaderRescue.after(SliceEnd.END_OF_LINE, ROOMY);

		feed(tested, "User-Agent: Fire", "fox/128.0\r\n", "\r\n");

		assertThat(tested.headers()).containsExactly(USER_AGENT);
	}

	@Test
	void joinsABlankLineSplitBetweenItsCarriageReturnAndItsLineFeed() {
		HeaderRescue tested = HeaderRescue.after(SliceEnd.END_OF_LINE, ROOMY);

		feed(tested, "User-Agent: Firefox/128.0\r\n", "\r", "\n");

		assertThat(tested.isComplete()).isTrue();
	}

	@Test
	void isCompleteAtOnceWhenTheRejectedLineWasTheBlankLineEndingTheHead() {
		HeaderRescue tested = HeaderRescue.after(SliceEnd.END_OF_HEAD, ROOMY);

		assertThat(tested.isComplete()).isTrue();
		assertThat(tested.headers()).isEmpty();
	}

	@Test
	void completesWithWhatItReadOnceTheBudgetIsExceeded() {
		HeaderRescue tested = HeaderRescue.after(SliceEnd.END_OF_LINE, 40);

		feed(tested, "User-Agent: Firefox/128.0\r\n", "X-Padding: " + "a".repeat(40) + "\r\n", "X-Acceptance-Client-Address: 203.0.113.7\r\n");

		assertThat(tested.isComplete()).isTrue();
		assertThat(tested.headers()).containsExactly(USER_AGENT);
	}

	@Test
	void carriesAHeaderLineThatFillsTheBudgetExactly() {
		String line = "User-Agent: Firefox/128.0\r\n";
		HeaderRescue tested = HeaderRescue.after(SliceEnd.END_OF_LINE, line.length());

		feed(tested, line);

		assertThat(tested.isComplete()).isFalse();
		assertThat(tested.headers()).containsExactly(USER_AGENT);
	}

	@Test
	void completesOnceTheRestOfTheRejectedLineExceedsTheBudgetToo() {
		HeaderRescue tested = HeaderRescue.after(SliceEnd.MID_LINE, 40);

		feed(tested, "a".repeat(41));

		assertThat(tested.isComplete()).isTrue();
	}

	@Test
	void ignoresLinesThatAreNoHeaderLines() {
		HeaderRescue tested = HeaderRescue.after(SliceEnd.END_OF_LINE, ROOMY);

		feed(tested, "GET / HTTP/1.1\r\n", " folded: continuation\r\n", "Spaced Name: value\r\n", ": nameless\r\n", "User-Agent: Firefox/128.0\r\n", "\r\n");

		assertThat(tested.headers()).containsExactly(USER_AGENT);
	}

	@Test
	void leavesOutALineEndedByABareLineFeedAndStillTakesABareLineFeedForTheEndOfTheHead() {
		HeaderRescue tested = HeaderRescue.after(SliceEnd.END_OF_LINE, ROOMY);

		feed(tested, "User-Agent: Firefox/128.0\n", "\n");

		assertThat(tested.headers()).isEmpty();
		assertThat(tested.isComplete()).isTrue();
	}

	@Test
	void trimsTheValueOfAHeaderLine() {
		HeaderRescue tested = HeaderRescue.after(SliceEnd.END_OF_LINE, ROOMY);

		feed(tested, "User-Agent:   Firefox/128.0  \r\n", "\r\n");

		assertThat(tested.headers()).containsExactly(USER_AGENT);
	}

	@Test
	void trimsTabsAroundTheValueOfAHeaderLineToo() {
		HeaderRescue tested = HeaderRescue.after(SliceEnd.END_OF_LINE, ROOMY);

		feed(tested, "User-Agent:\t Firefox/128.0 \t\r\n", "\r\n");

		assertThat(tested.headers()).containsExactly(USER_AGENT);
	}

	@Test
	void doesNotTrimAControlCharacterAroundTheValueOfAHeaderLine() {
		HeaderRescue tested = HeaderRescue.after(SliceEnd.END_OF_LINE, ROOMY);

		feed(tested, "User-Agent: \u001FFirefox/128.0\u001F\r\n", "\r\n");

		assertThat(tested.headers()).containsExactly(new RescuedHeader("User-Agent", "\u001FFirefox/128.0\u001F"));
	}

	@Test
	void ignoresSlicesOnceComplete() {
		HeaderRescue tested = HeaderRescue.after(SliceEnd.END_OF_LINE, ROOMY);
		feed(tested, "User-Agent: Firefox/128.0\r\n", "\r\n");

		feed(tested, "X-Acceptance-Client-Address: 203.0.113.7\r\n");

		assertThat(tested.headers()).containsExactly(USER_AGENT);
	}

	private static void feed(HeaderRescue tested, String... slices) {
		List.of(slices).forEach(slice -> tested.feed(slice.getBytes(ISO_8859_1)));
	}
}
