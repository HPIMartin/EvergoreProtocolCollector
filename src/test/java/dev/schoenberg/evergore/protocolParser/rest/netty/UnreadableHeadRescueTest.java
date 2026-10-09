package dev.schoenberg.evergore.protocolParser.rest.netty;

import java.util.ArrayList;
import java.util.List;

import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static dev.schoenberg.evergore.protocolParser.rest.netty.HeadChannel.DEFAULT_MAX_HEADER_SIZE;
import static dev.schoenberg.evergore.protocolParser.rest.netty.HeadChannel.receive;
import static org.assertj.core.api.Assertions.assertThat;

class UnreadableHeadRescueTest {
	private static final String ACCEPTANCE_CLIENT_HEADER = "X-Acceptance-Client-Address";
	private static final String REQUEST_LINE_OVER_THE_LIMIT = "GET /" + "a".repeat(5000) + " HTTP/1.1\r\n";
	private static final String HOST_LINE = "Host: localhost\r\n";
	private static final String USER_AGENT_LINE = "User-Agent: Firefox/128.0\r\n";
	private static final String ACCEPTANCE_CLIENT_LINE = ACCEPTANCE_CLIENT_HEADER + ": 203.0.113.7\r\n";
	private static final String OVERSIZED_HEADER_LINE = "X-Padding: " + "a".repeat(9000) + "\r\n";

	private final EmbeddedChannel channel = HeadChannel.withMaxHeaderSize(DEFAULT_MAX_HEADER_SIZE);

	@AfterEach
	void closeChannel() {
		channel.finishAndReleaseAll();
	}

	@Test
	void carriesTheHeadersThatFollowARequestLineOverTheLimitInTheSameChunk() {
		receive(channel, REQUEST_LINE_OVER_THE_LIMIT + USER_AGENT_LINE + ACCEPTANCE_CLIENT_LINE + "Connection: close\r\n\r\n");

		UnreadableRequest standIn = onlyStandIn();

		assertThat(standIn.status().code()).isEqualTo(413);
		assertThat(standIn.headers().get("User-Agent")).isEqualTo("Firefox/128.0");
		assertThat(standIn.headers().get(ACCEPTANCE_CLIENT_HEADER)).isEqualTo("203.0.113.7");
	}

	@Test
	void waitsForTheHeadersThatArriveInLaterChunks() {
		receive(channel, REQUEST_LINE_OVER_THE_LIMIT);
		assertThat(nextPassedOn()).isNull();
		receive(channel, USER_AGENT_LINE);
		assertThat(nextPassedOn()).isNull();

		receive(channel, ACCEPTANCE_CLIENT_LINE + "\r\n");
		UnreadableRequest standIn = onlyStandIn();

		assertThat(standIn.headers().get("User-Agent")).isEqualTo("Firefox/128.0");
		assertThat(standIn.headers().get(ACCEPTANCE_CLIENT_HEADER)).isEqualTo("203.0.113.7");
	}

	@Test
	void joinsAHeaderLineSplitAcrossChunks() {
		receive(channel, REQUEST_LINE_OVER_THE_LIMIT + "User-Agent: Fire");

		receive(channel, "fox/128.0\r\n\r\n");
		UnreadableRequest standIn = onlyStandIn();

		assertThat(standIn.headers().get("User-Agent")).isEqualTo("Firefox/128.0");
	}

	@Test
	void carriesTheHeadersOfTheRejectedRequestAndNotOfTheServedOneBeforeItInTheSameChunk() {
		receive(channel, "GET / HTTP/1.1\r\nUser-Agent: first/1.0\r\n\r\n" + REQUEST_LINE_OVER_THE_LIMIT + "User-Agent: second/2.0\r\n\r\n");

		List<Object> passedOn = everythingPassedOn();

		assertThat(passedOn.getFirst()).isInstanceOfSatisfying(HttpRequest.class, served -> assertThat(served.headers().get("User-Agent")).isEqualTo("first/1.0"));
		assertThat(passedOn.getLast()).isInstanceOfSatisfying(UnreadableRequest.class, standIn -> assertThat(standIn.headers().get("User-Agent")).isEqualTo("second/2.0"));
	}

	@Test
	void doesNotRejectAServedRequestWhoseBodyHasNoLineEndAndCarriesTheHeadersOfTheRequestAfterIt() {
		receive(channel, "POST / HTTP/1.1\r\nContent-Length: 6000\r\n\r\n" + "b".repeat(6000) + REQUEST_LINE_OVER_THE_LIMIT + "User-Agent: second/2.0\r\n\r\n");

		List<Object> passedOn = everythingPassedOn();

		assertThat(passedOn.getFirst()).isInstanceOfSatisfying(HttpRequest.class, served -> assertThat(served.method()).isEqualTo(HttpMethod.POST));
		assertThat(passedOn.stream().filter(UnreadableRequest.class::isInstance)).hasSize(1);
		assertThat(passedOn.getLast()).isInstanceOfSatisfying(UnreadableRequest.class, standIn -> assertThat(standIn.headers().get("User-Agent")).isEqualTo("second/2.0"));
	}

	@Test
	void leavesOutARescuedHeaderTheServerWouldNotHaveReadAndKeepsTheOthers() {
		receive(channel, REQUEST_LINE_OVER_THE_LIMIT + "X-Injected: first\u0001second\r\nBad(Name): value\r\n" + USER_AGENT_LINE + "\r\n");

		UnreadableRequest standIn = onlyStandIn();

		assertThat(standIn.headers().names()).map(String::toLowerCase).containsExactly("user-agent", "connection");
	}

	@Test
	void passesTheStandInOnWithWhatItReadOnceTheHeaderBudgetIsExceeded() {
		EmbeddedChannel small = HeadChannel.withMaxHeaderSize(64);
		receive(small, REQUEST_LINE_OVER_THE_LIMIT + USER_AGENT_LINE + "X-Padding: " + "a".repeat(100) + "\r\n" + ACCEPTANCE_CLIENT_LINE + "\r\n");

		UnreadableRequest standIn = (UnreadableRequest) small.readInbound();

		assertThat(standIn.headers().get("User-Agent")).isEqualTo("Firefox/128.0");
		assertThat(standIn.headers().contains(ACCEPTANCE_CLIENT_HEADER)).isFalse();
		small.finishAndReleaseAll();
	}

	@Test
	void passesTheStandInOnOnceTheRestOfTheRejectedLineExceedsTheHeaderBudget() {
		receive(channel, "GET /" + "a".repeat(5000));
		assertThat(nextPassedOn()).isNull();

		receive(channel, "a".repeat(DEFAULT_MAX_HEADER_SIZE + 1));
		UnreadableRequest standIn = onlyStandIn();

		assertThat(standIn.status().code()).isEqualTo(413);
	}

	@Test
	void chargesTheHeldPartOfARejectedHeaderLineAgainstTheHeaderBudget() {
		receive(channel, "GET / HTTP/1.1\r\nHost: x\r\nCookie: " + "a".repeat(4000) + "\r\nX-Padding: " + "a".repeat(5000));
		assertThat(nextPassedOn()).isNull();

		receive(channel, "a".repeat(3200));
		UnreadableRequest standIn = onlyStandIn();

		assertThat(standIn.status().code()).isEqualTo(413);
	}

	@Test
	void passesExactlyOneStandInOnHoweverMuchTheClientPipelinesBehindTheRejectedHead() {
		String pipelined = "GET /favicon.ico HTTP/1.1\r\n" + HOST_LINE + "\r\n";
		receive(channel, REQUEST_LINE_OVER_THE_LIMIT + USER_AGENT_LINE + "\r\n" + pipelined + pipelined);

		List<Object> passedOn = everythingPassedOn();

		assertThat(passedOn).hasSize(1).first().isInstanceOf(UnreadableRequest.class);
	}

	@Test
	void neverReadsTheRejectedRequestLineAsAHeader() {
		receive(channel, "User-Agent:GET /overview?token=secret" + "a".repeat(5000) + " HTTP/1.1\r\n\r\n");

		UnreadableRequest standIn = onlyStandIn();

		assertThat(standIn.toString()).doesNotContain("secret");
	}

	@Test
	void neverCarriesTheRejectedTargetOfARequestThatFollowsABodyWithoutLineEnd() {
		receive(channel, "POST / HTTP/1.1\r\nContent-Length: 11\r\n\r\nUser-Agent:" + "GET /x?token=secret" + "a".repeat(5000) + " HTTP/1.1\r\n\r\n");

		List<Object> passedOn = everythingPassedOn();

		assertThat(passedOn.getLast()).isInstanceOf(UnreadableRequest.class);
		assertThat(passedOn.getLast().toString()).doesNotContain("token=secret");
	}

	@Test
	void passesNothingOnWhenTheChannelClosesBeforeTheBlankLine() {
		receive(channel, REQUEST_LINE_OVER_THE_LIMIT + USER_AGENT_LINE);

		channel.close();

		assertThat(nextPassedOn()).isNull();
	}

	@Test
	void carriesTheHeadersBeforeAndAfterARejectedHeaderLine() {
		receive(channel, "GET / HTTP/1.1\r\nUser-Agent: Firefox/128.0\r\nSpaced Name: rejected\r\n" + ACCEPTANCE_CLIENT_LINE + "\r\n");

		UnreadableRequest standIn = onlyStandIn();

		assertThat(standIn.status().code()).isEqualTo(400);
		assertThat(standIn.headers().get("User-Agent")).isEqualTo("Firefox/128.0");
		assertThat(standIn.headers().get(ACCEPTANCE_CLIENT_HEADER)).isEqualTo("203.0.113.7");
	}

	@Test
	void carriesTheHeaderRightBeforeAnOversizedHeaderLine() {
		receive(channel, "GET / HTTP/1.1\r\n" + HOST_LINE + USER_AGENT_LINE + OVERSIZED_HEADER_LINE + "\r\n");

		UnreadableRequest standIn = onlyStandIn();

		assertThat(standIn.status().code()).isEqualTo(413);
		assertThat(standIn.headers().get("User-Agent")).isEqualTo("Firefox/128.0");
	}

	@Test
	void carriesAHeaderBeforeAnOversizedHeaderLineEvenWhenItMentionsTheMethodAndTheTarget() {
		receive(channel, "GET / HTTP/1.1\r\n" + HOST_LINE + "User-Agent: Mozilla/5.0 GADGET\r\n" + OVERSIZED_HEADER_LINE + "\r\n");

		UnreadableRequest standIn = onlyStandIn();

		assertThat(standIn.headers().get("User-Agent")).isEqualTo("Mozilla/5.0 GADGET");
	}

	@Test
	void carriesTheAcceptanceClientAddressRightBeforeAnOversizedHeaderLine() {
		receive(channel, "GET / HTTP/1.1\r\n" + HOST_LINE + ACCEPTANCE_CLIENT_LINE + OVERSIZED_HEADER_LINE + "\r\n");

		UnreadableRequest standIn = onlyStandIn();

		assertThat(standIn.headers().get(ACCEPTANCE_CLIENT_HEADER)).isEqualTo("203.0.113.7");
	}

	@Test
	void carriesTheWholeLineTheLateRejectionHappenedOnWhenItArrivedInPieces() {
		receive(channel, "GET / HTTP/1.1\r\nSpaced Name: x\r\nUser-Ag");

		receive(channel, "ent: Firefox/128.0\r\n\r\n");
		UnreadableRequest standIn = onlyStandIn();

		assertThat(standIn.headers().get("User-Agent")).isEqualTo("Firefox/128.0");
	}

	@Test
	void carriesTheUserAgentLineTheDecoderFailedOnWhenItArrivesInPieces() {
		receive(channel, "GET / HTTP/1.1\r\nHost: x\r\nCookie: " + "a".repeat(8175) + "\r\nUser-Ag");

		receive(channel, "ent: Firefox/128.0\r\n\r\n");
		UnreadableRequest standIn = onlyStandIn();

		assertThat(standIn.status().code()).isEqualTo(413);
		assertThat(standIn.headers().get("User-Agent")).isEqualTo("Firefox/128.0");
	}

	@Test
	void carriesTheHeaderRightBeforeAnOversizedHeaderLineThatHasNotEndedYet() {
		receive(channel, "GET / HTTP/1.1\r\n" + HOST_LINE + USER_AGENT_LINE + "X-Padding: " + "a".repeat(9000));
		assertThat(nextPassedOn()).isNull();

		receive(channel, "\r\n\r\n");
		UnreadableRequest standIn = onlyStandIn();

		assertThat(standIn.headers().get("User-Agent")).isEqualTo("Firefox/128.0");
	}

	@Test
	void doesNotCarryAnOversizedHeaderLineThatHasNotEndedYetWhenItExceedsTheBudget() {
		receive(channel, "GET / HTTP/1.1\r\n" + HOST_LINE + USER_AGENT_LINE + "X-Padding: " + "a".repeat(9000));

		receive(channel, "\r\n\r\n");
		UnreadableRequest standIn = onlyStandIn();

		assertThat(standIn.headers().get("User-Agent")).isEqualTo("Firefox/128.0");
		assertThat(standIn.headers().contains("X-Padding")).isFalse();
	}

	@ParameterizedTest(name = "{displayName} [{index}]")
	@ValueSource(strings = {"GET /x?token=secret HTTP/1.1\r\n", "GET /x?token=secret  HTTP/1.1\r\n", "GET\t/x?token=secret\tHTTP/1.1\r\n", "GET /x?token=secret http/1.1\r\n"})
	void neverReadsTheRequestLineBeforeARejectedHeaderLineAsAHeaderHoweverTheClientSpacedIt(String requestLine) {
		receive(channel, "POST / HTTP/1.1\r\nContent-Length: 11\r\n\r\nUser-Agent:" + requestLine + OVERSIZED_HEADER_LINE + "\r\n");

		List<Object> passedOn = everythingPassedOn();

		assertThat(passedOn.getLast()).isInstanceOf(UnreadableRequest.class);
		assertThat(passedOn.getLast().toString()).doesNotContain("secret");
	}

	@Test
	void neverReadsARejectedRequestLineThatEndsWithASpaceAsAHeader() {
		receive(channel, "POST / HTTP/1.1\r\nContent-Length: 11\r\n\r\nUser-Agent:" + "GET /x?token=secret HTTP/1.1 \r\n" + OVERSIZED_HEADER_LINE + "\r\n");

		List<Object> passedOn = everythingPassedOn();

		assertThat(passedOn.getLast()).isInstanceOf(UnreadableRequest.class);
		assertThat(passedOn.getLast().toString()).doesNotContain("secret");
	}

	@Test
	void doesNotCarryTheFirstHeaderWhenTheSecondHeaderLineIsTheOversizedOne() {
		receive(channel, "GET / HTTP/1.1\r\n" + USER_AGENT_LINE + OVERSIZED_HEADER_LINE + "\r\n");

		UnreadableRequest standIn = onlyStandIn();

		assertThat(standIn.status().code()).isEqualTo(413);
		assertThat(standIn.headers().contains("User-Agent")).isFalse();
	}

	@Test
	void carriesAHeaderOnceWhenTheDecoderHadReadItBeforeTheLineItRejected() {
		receive(channel, "GET / HTTP/1.1\r\n" + USER_AGENT_LINE + "Not a header line\r\n\r\n");

		UnreadableRequest standIn = onlyStandIn();

		assertThat(standIn.headers().getAll("User-Agent")).containsExactly("Firefox/128.0");
	}

	@Test
	void passesTheStandInOnAtOnceWhenTheHeadFailsAtItsBlankLineEvenWhenThatLineArrivesInPieces() {
		receive(channel, "GET / HTTP/1.1\r\n" + USER_AGENT_LINE + "Content-Length: 5\r\nContent-Length: 6\r\n\r");
		assertThat(nextPassedOn()).isNull();

		receive(channel, "\n");
		UnreadableRequest standIn = onlyStandIn();

		assertThat(standIn.headers().get("User-Agent")).isEqualTo("Firefox/128.0");
	}

	@Test
	void doesNotTakeALineOfSeveralCarriageReturnsForTheBlankLineThatEndsTheHead() {
		receive(channel, "GET / HTTP/1.1\r\nSpaced Name: x\r\n\r\r\n" + "User-Agent: late\r\n\r\n");

		UnreadableRequest standIn = onlyStandIn();

		assertThat(standIn.headers().get("User-Agent")).isEqualTo("late");
	}

	@Test
	void doesNotTakeATwoByteLineTheServerRejectsForTheBlankLineThatEndsTheHead() {
		receive(channel, "GET / HTTP/1.1\r\n" + USER_AGENT_LINE + "b\n" + ACCEPTANCE_CLIENT_LINE + "\r\n");

		UnreadableRequest standIn = onlyStandIn();

		assertThat(standIn.status().code()).isEqualTo(400);
		assertThat(standIn.headers().get(ACCEPTANCE_CLIENT_HEADER)).isEqualTo("203.0.113.7");
	}

	@Test
	void keepsTheStandInWithinTheHeaderBudget() {
		receive(channel, "GET / HTTP/1.1\r\nA: " + "a".repeat(4000) + "\r\nB: " + "b".repeat(4000) + "\r\nC: " + "c".repeat(4000) + "\r\nD: " + "d".repeat(8000) + "\r\n\r\n");

		UnreadableRequest standIn = onlyStandIn();
		int sizeOfHeaders = standIn.headers().entries().stream().mapToInt(header -> header.getKey().length() + header.getValue().length() + 4).sum();

		assertThat(sizeOfHeaders).isLessThanOrEqualTo(DEFAULT_MAX_HEADER_SIZE);
	}

	@Test
	void keepsTheBrowserWhenCookiesBeforeItFillTheHeaderBudget() {
		receive(channel,
				"GET / HTTP/1.1\r\n" + HOST_LINE + "Cookie: " + "a".repeat(4000) + "\r\nCookie: " + "b".repeat(4125) + "\r\nAccept: text/html\r\n" + USER_AGENT_LINE + "\r\n");

		UnreadableRequest standIn = onlyStandIn();

		assertThat(standIn.headers().get("User-Agent")).isEqualTo("Firefox/128.0");
	}

	@Test
	void keepsTheBrowserWhenOnePaddingHeaderFillsTheHeaderBudget() {
		receive(channel, "GET / HTTP/1.1\r\nHost: x\r\nX-Padding: " + "a".repeat(8150) + "\r\n" + USER_AGENT_LINE + "\r\n");

		UnreadableRequest standIn = onlyStandIn();

		assertThat(standIn.headers().get("User-Agent")).isEqualTo("Firefox/128.0");
	}

	@ParameterizedTest(name = "{displayName} [{index}]")
	@ValueSource(strings = {"User-Agent: \u001Fcurl/8.9\r\n", "User-Agent:\u001Ccurl/8.9\r\n", "User-Agent: curl/8.9\u001F\r\n"})
	void leavesOutARescuedHeaderWhoseValueHoldsAControlCharacterTheServerDoesNotTrim(String headerLine) {
		receive(channel, REQUEST_LINE_OVER_THE_LIMIT + headerLine + ACCEPTANCE_CLIENT_LINE + "\r\n");

		UnreadableRequest standIn = onlyStandIn();

		assertThat(standIn.headers().contains("User-Agent")).isFalse();
		assertThat(standIn.headers().get(ACCEPTANCE_CLIENT_HEADER)).isEqualTo("203.0.113.7");
	}

	@ParameterizedTest(name = "{displayName} [{index}]")
	@ValueSource(strings = {"User-Agent: \u001Fcurl/8.9\r\n", "User-Agent: curl/8.9\u001F\r\n", "Bad(Name): value\r\n"})
	void leavesOutTheHeaderLineTheServerRejectedAtTheBlankLineThatFollowsIt(String headerLine) {
		receive(channel, "GET / HTTP/1.1\r\n" + HOST_LINE + headerLine + "\r\n");

		UnreadableRequest standIn = onlyStandIn();

		assertThat(standIn.status().code()).isEqualTo(400);
		assertThat(standIn.headers().names()).map(String::toLowerCase).containsExactly("host", "connection");
	}

	@Test
	void doesNotCarryTheHeaderBeforeALineEndedByABareLineFeedTheServerRejectedBeforeReadingIt() {
		receive(channel, "GET / HTTP/1.1\r\n" + USER_AGENT_LINE + "X-Bare: value\n" + ACCEPTANCE_CLIENT_LINE + "\r\n");

		UnreadableRequest standIn = onlyStandIn();

		assertThat(standIn.status().code()).isEqualTo(400);
		assertThat(standIn.headers().contains("User-Agent")).isFalse();
		assertThat(standIn.headers().get(ACCEPTANCE_CLIENT_HEADER)).isEqualTo("203.0.113.7");
	}

	@Test
	void leavesOutARescuedHeaderLineEndedByABareLineFeedAsTheServerWould() {
		receive(channel, REQUEST_LINE_OVER_THE_LIMIT + "X-Bare: value\n" + USER_AGENT_LINE + "\r\n");

		UnreadableRequest standIn = onlyStandIn();

		assertThat(standIn.headers().contains("X-Bare")).isFalse();
		assertThat(standIn.headers().get("User-Agent")).isEqualTo("Firefox/128.0");
	}

	@Test
	void carriesTheHeaderRightBeforeABlankLineEndedByABareLineFeed() {
		receive(channel, "GET / HTTP/1.1\r\n" + HOST_LINE + USER_AGENT_LINE + "\n");

		UnreadableRequest standIn = onlyStandIn();

		assertThat(standIn.status().code()).isEqualTo(400);
		assertThat(standIn.headers().get("User-Agent")).isEqualTo("Firefox/128.0");
	}

	@Test
	void doesNotCarryAFoldedHeaderRightBeforeAnOversizedHeaderLine() {
		receive(channel, "GET / HTTP/1.1\r\n" + HOST_LINE + "User-Agent: Firefox\r\n /128.0\r\n" + OVERSIZED_HEADER_LINE + "\r\n");

		UnreadableRequest standIn = onlyStandIn();

		assertThat(standIn.status().code()).isEqualTo(413);
		assertThat(standIn.headers().contains("User-Agent")).isFalse();
	}

	@Test
	void carriesTheFirstLineOnlyOfAFoldedHeaderThatFollowsARejectedRequestLine() {
		receive(channel, REQUEST_LINE_OVER_THE_LIMIT + "User-Agent: Firefox\r\n /128.0\r\n\r\n");

		UnreadableRequest standIn = onlyStandIn();

		assertThat(standIn.headers().getAll("User-Agent")).containsExactly("Firefox");
	}

	private UnreadableRequest onlyStandIn() {
		List<Object> passedOn = everythingPassedOn();

		assertThat(passedOn).hasSize(1).first().isInstanceOf(UnreadableRequest.class);
		return (UnreadableRequest) passedOn.getFirst();
	}

	private Object nextPassedOn() {
		return channel.readInbound();
	}

	private List<Object> everythingPassedOn() {
		List<Object> passedOn = new ArrayList<>();
		for (Object message = nextPassedOn(); message != null; message = nextPassedOn()) {
			passedOn.add(message);
		}
		return passedOn;
	}
}
