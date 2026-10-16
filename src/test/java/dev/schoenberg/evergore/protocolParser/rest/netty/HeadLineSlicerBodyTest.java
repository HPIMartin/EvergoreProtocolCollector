package dev.schoenberg.evergore.protocolParser.rest.netty;

import java.util.ArrayList;
import java.util.List;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.ReferenceCountUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static dev.schoenberg.evergore.protocolParser.rest.netty.HeadChannel.DEFAULT_MAX_HEADER_SIZE;
import static dev.schoenberg.evergore.protocolParser.rest.netty.HeadChannel.receive;
import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class HeadLineSlicerBodyTest {
	private static final String BODY_OF_LINE_FEEDS = "\n".repeat(1000);
	private static final String HEAD_WITH_A_BODY = "POST /login HTTP/1.1\r\nHost: localhost\r\nContent-Length: " + BODY_OF_LINE_FEEDS.length() + "\r\n\r\n";
	private static final String CHUNKED_HEAD = "POST /login HTTP/1.1\r\nHost: localhost\r\nTransfer-Encoding: chunked\r\n\r\n";
	private static final String SHORT_CHUNKED_REQUEST = CHUNKED_HEAD + "5\r\nab\ncd\r\n0\r\n\r\n";
	private static final String READABLE_HEAD = "GET / HTTP/1.1\r\nHost: localhost\r\n\r\n";
	private static final String SHORT_UNREADABLE_HEAD = "GET / HTTP/1.1\r\nSpaced Name: rejected\r\nUser-Agent: Firefox/128.0\r\n\r\n";
	private static final String UNREADABLE_HEAD = "GET /" + "a".repeat(5000) + " HTTP/1.1\r\nUser-Agent: Firefox/128.0\r\n\r\n";

	private final EmbeddedChannel tested = HeadChannel.withMaxHeaderSize(DEFAULT_MAX_HEADER_SIZE);
	private final List<Class<?>> reachingTheCodec = new ArrayList<>();

	@AfterEach
	void closeChannel() {
		tested.finishAndReleaseAll();
	}

	@Test
	void passesABodyBehindAReadableHeadToTheCodecAsItWasRead() {
		countWhatReachesTheCodec();

		receive(tested, HEAD_WITH_A_BODY + BODY_OF_LINE_FEEDS);
		int messagesReachingTheCodec = reachingTheCodec.size();
		releaseEverythingPassedOn();

		assertThat(messagesReachingTheCodec).isEqualTo(linesOf(HEAD_WITH_A_BODY) + 1);
	}

	@Test
	void passesAChunkedBodyToTheCodecAsItWasRead() {
		countWhatReachesTheCodec();

		receive(tested, CHUNKED_HEAD + "3e8\r\n" + BODY_OF_LINE_FEEDS + "\r\n0\r\n\r\n");
		int messagesReachingTheCodec = reachingTheCodec.size();
		releaseEverythingPassedOn();

		assertThat(messagesReachingTheCodec).isEqualTo(linesOf(CHUNKED_HEAD) + 1);
	}

	@Test
	void passesTheEmptyLinesBeforeAHeadToTheCodecAsTheyWereRead() {
		countWhatReachesTheCodec();

		receive(tested, BODY_OF_LINE_FEEDS + READABLE_HEAD);
		int messagesReachingTheCodec = reachingTheCodec.size();
		releaseEverythingPassedOn();

		assertThat(messagesReachingTheCodec).isEqualTo(linesOf(READABLE_HEAD) + 1);
	}

	@Test
	void passesTheEmptyLinesAfterARequestToTheCodecAsTheyWereRead() {
		countWhatReachesTheCodec();

		receive(tested, READABLE_HEAD + BODY_OF_LINE_FEEDS);
		int messagesReachingTheCodec = reachingTheCodec.size();
		releaseEverythingPassedOn();

		assertThat(messagesReachingTheCodec).isEqualTo(linesOf(READABLE_HEAD) + 1);
	}

	@Test
	void passesTheBlankLinesAfterABodyOfAKnownLengthToTheCodecAsTheyWereRead() {
		countWhatReachesTheCodec();

		receive(tested, HEAD_WITH_A_BODY + BODY_OF_LINE_FEEDS + " \n".repeat(500) + READABLE_HEAD);
		int messagesReachingTheCodec = reachingTheCodec.size();
		releaseEverythingPassedOn();

		assertThat(messagesReachingTheCodec).isEqualTo(linesOf(HEAD_WITH_A_BODY) + 1 + 1 + linesOf(READABLE_HEAD));
	}

	@Test
	void passesEveryByteUpToTheSpaceBeforeAHeadToTheCodecAsItWasRead() {
		countWhatReachesTheCodec();

		tested.writeInbound(Unpooled.copiedBuffer(" \t\u0000\u001f\r\n".repeat(200) + READABLE_HEAD, ISO_8859_1));
		int messagesReachingTheCodec = reachingTheCodec.size();
		releaseEverythingPassedOn();

		assertThat(messagesReachingTheCodec).isEqualTo(linesOf(READABLE_HEAD) + 1);
	}

	@Test
	void passesTheBlankLookingLinesAfterAChunkedBodyToTheCodecAsTheyWereRead() {
		receive(tested, SHORT_CHUNKED_REQUEST);
		releaseEverythingPassedOn();
		countWhatReachesTheCodec();

		receive(tested, " \r\n".repeat(1000) + READABLE_HEAD);
		int messagesReachingTheCodec = reachingTheCodec.size();
		releaseEverythingPassedOn();

		assertThat(messagesReachingTheCodec).isEqualTo(2 + linesOf(READABLE_HEAD));
	}

	@Test
	void rescuesTheBrowserBehindMoreLeadingLineEndsThanTheCodecSkips() {
		receive(tested, "\n".repeat(5000) + "GET / HTTP/1.1\r\nUser-Agent: Firefox/128.0\r\n\r\n");
		List<Object> passedOn = everythingPassedOn();

		assertThat(passedOn).singleElement().isInstanceOfSatisfying(UnreadableRequest.class, standIn -> {
			assertThat(standIn.status().code()).isEqualTo(413);
			assertThat(standIn.headers().get("User-Agent")).isEqualTo("Firefox/128.0");
		});
	}

	@Test
	void rescuesTheBrowserBehindMoreLeadingLineEndsThanTheCodecSkipsAcrossTwoReads() {
		receive(tested, "\n".repeat(5000));
		receive(tested, "\n".repeat(10) + "GET / HTTP/1.1\r\nUser-Agent: Firefox/128.0\r\n\r\n");
		List<Object> passedOn = everythingPassedOn();

		assertThat(passedOn).singleElement().isInstanceOfSatisfying(UnreadableRequest.class, standIn -> assertThat(standIn.headers().get("User-Agent")).isEqualTo("Firefox/128.0"));
	}

	@Test
	void neverCarriesTheLineAfterMoreLeadingLineEndsThanTheCodecSkipsAsAHeader() {
		receive(tested, "\n".repeat(5000) + "\u0001User-Agent: Spoof\r\nUser-Agent: Firefox/128.0\r\n\r\n");
		List<Object> passedOn = everythingPassedOn();

		assertThat(passedOn).singleElement().isInstanceOfSatisfying(UnreadableRequest.class, standIn -> assertThat(standIn.headers().get("User-Agent")).isEqualTo("Firefox/128.0"));
	}

	@Test
	void answersOnceIdleBytesAfterARejectionExceedTheHeaderBudget() {
		receive(tested, "\n".repeat(5000));
		receive(tested, " ".repeat(DEFAULT_MAX_HEADER_SIZE + 1));
		List<Object> passedOn = everythingPassedOn();

		assertThat(passedOn).singleElement().isInstanceOf(UnreadableRequest.class);
	}

	@Test
	void answersARequestLineThatStartsWithAByteTheCodecDoesNotSkip() {
		tested.writeInbound(Unpooled.copiedBuffer("\u0080 \u0080 \u0080\r\n\r\n", ISO_8859_1));
		List<Object> passedOn = everythingPassedOn();

		assertThat(passedOn).singleElement().isInstanceOfSatisfying(UnreadableRequest.class, standIn -> assertThat(standIn.status().code()).isEqualTo(400));
	}

	@Test
	void rescuesTheBrowserBehindARequestLineThatStartsWithAByteTheCodecDoesNotSkip() {
		tested.writeInbound(Unpooled.copiedBuffer("\u007f \u007f \u007f\r\nUser-Agent: Firefox/128.0\r\n\r\n", ISO_8859_1));
		List<Object> passedOn = everythingPassedOn();

		assertThat(passedOn).singleElement().isInstanceOfSatisfying(UnreadableRequest.class, standIn -> assertThat(standIn.headers().get("User-Agent")).isEqualTo("Firefox/128.0"));
	}

	@Test
	void passesTheBodyOfARejectedHeadToTheCodecAsItWasRead() {
		countWhatReachesTheCodec();
		String linesTheCodecReads = "POST / HTTP/1.1\r\nSpaced Name: rejected\r\nContent-Length: 1000\r\n";

		receive(tested, linesTheCodecReads + "\r\n" + "x\n".repeat(500));
		int messagesReachingTheCodec = reachingTheCodec.size();
		releaseEverythingPassedOn();

		assertThat(messagesReachingTheCodec).isEqualTo(linesOf(linesTheCodecReads) + 1);
	}

	@Test
	void passesEverythingAfterAnUnreadableChunkToTheCodecAsItWasRead() {
		countWhatReachesTheCodec();

		receive(tested, CHUNKED_HEAD + "zz\r\n");
		releaseEverythingPassedOn();
		int messagesBeforeTheLines = reachingTheCodec.size();

		receive(tested, "x\n".repeat(500));
		int messagesForTheLines = reachingTheCodec.size() - messagesBeforeTheLines;
		releaseEverythingPassedOn();

		assertThat(messagesForTheLines).isEqualTo(1);
	}

	@Test
	void slicesTheNextHeadAgainOnceABodyOfAKnownLengthHasEnded() {
		receive(tested, HEAD_WITH_A_BODY + BODY_OF_LINE_FEEDS);
		releaseEverythingPassedOn();

		receive(tested, SHORT_UNREADABLE_HEAD);
		List<Object> passedOn = everythingPassedOn();

		assertThat(passedOn).singleElement().isInstanceOfSatisfying(UnreadableRequest.class, standIn -> {
			assertThat(standIn.status().code()).isEqualTo(400);
			assertThat(standIn.headers().get("User-Agent")).isEqualTo("Firefox/128.0");
		});
	}

	@Test
	void slicesTheNextHeadAgainOnceAChunkedBodyHasEnded() {
		receive(tested, SHORT_CHUNKED_REQUEST);
		releaseEverythingPassedOn();

		receive(tested, UNREADABLE_HEAD);
		List<Object> passedOn = everythingPassedOn();

		assertThat(passedOn).singleElement().isInstanceOfSatisfying(UnreadableRequest.class, standIn -> assertThat(standIn.headers().get("User-Agent")).isEqualTo("Firefox/128.0"));
	}

	@Test
	void slicesTheNextHeadAfterARequestWithoutABody() {
		receive(tested, READABLE_HEAD);
		releaseEverythingPassedOn();

		receive(tested, UNREADABLE_HEAD);
		List<Object> passedOn = everythingPassedOn();

		assertThat(passedOn).singleElement().isInstanceOfSatisfying(UnreadableRequest.class, standIn -> assertThat(standIn.headers().get("User-Agent")).isEqualTo("Firefox/128.0"));
	}

	@Test
	void rescuesTheBrowserOfAnUnreadableHeadInTheSameReadBehindABodyOfAKnownLength() {
		receive(tested, HEAD_WITH_A_BODY + BODY_OF_LINE_FEEDS + UNREADABLE_HEAD);
		List<Object> passedOn = everythingPassedOn();

		assertThat(passedOn).last().isInstanceOfSatisfying(UnreadableRequest.class, standIn -> {
			assertThat(standIn.status().code()).isEqualTo(413);
			assertThat(standIn.headers().get("User-Agent")).isEqualTo("Firefox/128.0");
		});
	}

	@Test
	void rescuesTheBrowserOfAnUnreadableHeadBehindABodyOfAKnownLengthThatSpansTwoReads() {
		receive(tested, HEAD_WITH_A_BODY + BODY_OF_LINE_FEEDS.substring(0, 100));
		releaseEverythingPassedOn();

		receive(tested, BODY_OF_LINE_FEEDS.substring(100) + SHORT_UNREADABLE_HEAD);
		List<Object> passedOn = everythingPassedOn();

		assertThat(passedOn).last().isInstanceOfSatisfying(UnreadableRequest.class, standIn -> assertThat(standIn.headers().get("User-Agent")).isEqualTo("Firefox/128.0"));
	}

	@Test
	void rescuesTheBrowserOfAnUnreadableHeadBehindABodyTheCodecStartedInTheReadOfAChunkedBody() {
		receive(tested, SHORT_CHUNKED_REQUEST + HEAD_WITH_A_BODY + BODY_OF_LINE_FEEDS.substring(0, 100));
		releaseEverythingPassedOn();

		receive(tested, BODY_OF_LINE_FEEDS.substring(100) + SHORT_UNREADABLE_HEAD);
		List<Object> passedOn = everythingPassedOn();

		assertThat(passedOn).last().isInstanceOfSatisfying(UnreadableRequest.class, standIn -> assertThat(standIn.headers().get("User-Agent")).isEqualTo("Firefox/128.0"));
	}

	@Test
	void slicesTheNextHeadAgainAfterAChunkedHttp10BodyThatAlsoDeclaredALength() {
		receive(tested, "POST / HTTP/1.0\r\nTransfer-Encoding: chunked\r\nContent-Length: 1000\r\n\r\n5\r\nhello\r\n0\r\n\r\n");
		releaseEverythingPassedOn();

		receive(tested, SHORT_UNREADABLE_HEAD);
		List<Object> passedOn = everythingPassedOn();

		assertThat(passedOn).singleElement().isInstanceOfSatisfying(UnreadableRequest.class, standIn -> assertThat(standIn.headers().get("User-Agent")).isEqualTo("Firefox/128.0"));
	}

	@Test
	void answersAnUnreadableHeadWhoseBlankLineFollowsAChunkedBodyInTheNextRead() {
		receive(tested, SHORT_CHUNKED_REQUEST + "GET / HTTP/1.1\r\nHost: x\r\nContent-Length: abc\r\nUser-Agent: Firefox/128.0\r\n");
		releaseEverythingPassedOn();

		receive(tested, "\r\n");
		List<Object> passedOn = everythingPassedOn();

		assertThat(passedOn).singleElement().isInstanceOfSatisfying(UnreadableRequest.class, standIn -> {
			assertThat(standIn.status().code()).isEqualTo(400);
			assertThat(standIn.headers().get("User-Agent")).isEqualTo("Firefox/128.0");
		});
	}

	@Test
	void answersAnUnreadableHeadWhoseRestAfterAChunkedBodyIsOnlyLineEnds() {
		receive(tested, SHORT_CHUNKED_REQUEST + "GET / HTTP/1.1\r\nSpaced Name: rejected");
		releaseEverythingPassedOn();

		receive(tested, "\r\n\r\n");
		List<Object> passedOn = everythingPassedOn();

		assertThat(passedOn).singleElement().isInstanceOf(UnreadableRequest.class);
	}

	@Test
	void closesQuietlyOnAHeadCutOffWithALengthThatIsNoNumber() {
		receive(tested, "GET / HTTP/1.1\r\nContent-Length: abc\r\nUser-Agent: Firefox/128.0\r\n");

		Throwable failure = catchThrowable(tested::finish);
		List<Object> passedOn = everythingPassedOn();

		assertThat(failure).isNull();
		assertThat(passedOn).noneMatch(UnreadableRequest.class::isInstance);
	}

	@Test
	void leavesOutAControlCharacterHeaderOfAHeadThatStartedInTheReadOfAChunkedBody() {
		receive(tested, SHORT_CHUNKED_REQUEST + "GET / HTTP/1.1\r\nHost: x\r\n");
		releaseEverythingPassedOn();

		tested.writeInbound(Unpooled.copiedBuffer("\u0001Bad: v\r\nUser-Agent: Firefox/128.0\r\n\r\n", ISO_8859_1));
		List<Object> passedOn = everythingPassedOn();

		assertThat(passedOn).singleElement().isInstanceOfSatisfying(UnreadableRequest.class, standIn -> {
			assertThat(standIn.headers().get("User-Agent")).isEqualTo("Firefox/128.0");
			assertThat(standIn.headers().contains("Bad")).isFalse();
		});
	}

	@Test
	void answersAnUnreadableHeadInTheSameReadBehindAChunkedBodyAtOnceWithoutItsBrowser() {
		receive(tested, SHORT_CHUNKED_REQUEST + UNREADABLE_HEAD);
		List<Object> passedOn = everythingPassedOn();

		assertThat(passedOn.stream().filter(UnreadableRequest.class::isInstance)).singleElement().isInstanceOfSatisfying(UnreadableRequest.class, standIn -> {
			assertThat(standIn.status().code()).isEqualTo(413);
			assertThat(standIn.headers().get("User-Agent")).isNull();
		});
	}

	@Test
	void rescuesTheBrowserOfARejectedHeadWithASpaceOnlyLineBeforeItsBlankLine() {
		receive(tested, "GET / HTTP/1.1\r\nSpaced Name: rejected\r\nUser-Agent: Firefox/128.0\r\n \r\n\r\n");
		List<Object> passedOn = everythingPassedOn();

		assertThat(passedOn).singleElement().isInstanceOfSatisfying(UnreadableRequest.class, standIn -> {
			assertThat(standIn.status().code()).isEqualTo(400);
			assertThat(standIn.headers().get("User-Agent")).isEqualTo("Firefox/128.0");
		});
	}

	@Test
	void rescuesTheBrowserOfARejectedRequestLineWithASpaceOnlyLineBeforeTheBlankLine() {
		receive(tested, "GET /" + "a".repeat(5000) + " HTTP/1.1\r\nUser-Agent: Firefox/128.0\r\n \r\n\r\n");
		List<Object> passedOn = everythingPassedOn();

		assertThat(passedOn).singleElement().isInstanceOfSatisfying(UnreadableRequest.class, standIn -> assertThat(standIn.headers().get("User-Agent")).isEqualTo("Firefox/128.0"));
	}

	@Test
	void leavesOutAControlCharacterHeaderBehindASpaceOnlyLine() {
		receive(tested, "GET / HTTP/1.1\r\nHost: x\r\n \r\n\u0001Bad: v\r\nUser-Agent: Firefox/128.0\r\n\r\n");
		List<Object> passedOn = everythingPassedOn();

		assertThat(passedOn).singleElement().isInstanceOfSatisfying(UnreadableRequest.class, standIn -> {
			assertThat(standIn.headers().get("User-Agent")).isEqualTo("Firefox/128.0");
			assertThat(standIn.headers().contains("Bad")).isFalse();
		});
	}

	@Test
	void neverCarriesAControlCharacterHeaderAsTheBrowserBehindASpaceOnlyLine() {
		receive(tested, "GET / HTTP/1.1\r\nHost: x\r\n \r\n\u0001User-Agent: Spoof\r\nUser-Agent: Firefox/128.0\r\n\r\n");
		List<Object> passedOn = everythingPassedOn();

		assertThat(passedOn).singleElement().isInstanceOfSatisfying(UnreadableRequest.class, standIn -> assertThat(standIn.headers().get("User-Agent")).isEqualTo("Firefox/128.0"));
	}

	@Test
	void leavesOutAControlCharacterHeaderBehindASpaceOnlyLineOfAHeadAfterAChunkedBody() {
		receive(tested, SHORT_CHUNKED_REQUEST);
		releaseEverythingPassedOn();

		receive(tested, "GET / HTTP/1.1\r\nHost: x\r\n \r\n\u0001Bad: v\r\nUser-Agent: Firefox/128.0\r\n\r\n");
		List<Object> passedOn = everythingPassedOn();

		assertThat(passedOn).singleElement().isInstanceOfSatisfying(UnreadableRequest.class, standIn -> {
			assertThat(standIn.headers().get("User-Agent")).isEqualTo("Firefox/128.0");
			assertThat(standIn.headers().contains("Bad")).isFalse();
		});
	}

	@Test
	void carriesTheBrowserAfterAnOversizedContinuationBehindASpaceOnlyLine() {
		receive(tested, "GET / HTTP/1.1\r\nHost: x\r\n \r\n" + " ".repeat(9000) + "\r\nUser-Agent: Firefox/128.0\r\n\r\n");
		List<Object> passedOn = everythingPassedOn();

		assertThat(passedOn).singleElement().isInstanceOfSatisfying(UnreadableRequest.class, standIn -> assertThat(standIn.headers().get("User-Agent")).isEqualTo("Firefox/128.0"));
	}

	@Test
	void neverCarriesAFoldedLineAsAHeaderBehindASpaceOnlyLine() {
		receive(tested, "GET /" + "a".repeat(5000) + " HTTP/1.1\r\n \r\n\tUser-Agent: Evil/1.0\r\n\r\n");
		List<Object> passedOn = everythingPassedOn();

		assertThat(passedOn).singleElement().isInstanceOfSatisfying(UnreadableRequest.class, standIn -> assertThat(standIn.headers().contains("User-Agent")).isFalse());
	}

	@Test
	void answersOnceTheLineEndsAfterARejectionExceedTheHeaderBudget() {
		receive(tested, "\n".repeat(5000));
		receive(tested, "\n".repeat(DEFAULT_MAX_HEADER_SIZE + 1));
		List<Object> passedOn = everythingPassedOn();

		assertThat(passedOn).singleElement().isInstanceOf(UnreadableRequest.class);
	}

	private void countWhatReachesTheCodec() {
		tested.pipeline().addBefore(UnreadableHeadCustomizer.CODEC, "counter", new ChannelInboundHandlerAdapter() {
			@Override
			public void channelRead(ChannelHandlerContext context, Object message) {
				reachingTheCodec.add(message.getClass());
				context.fireChannelRead(message);
			}
		});
	}

	private static int linesOf(String head) {
		return head.split("\n", -1).length - 1;
	}

	private List<Object> everythingPassedOn() {
		List<Object> passedOn = new ArrayList<>();
		for (Object message = tested.readInbound(); message != null; message = tested.readInbound()) {
			passedOn.add(message);
		}
		passedOn.stream().filter(message -> !(message instanceof UnreadableRequest)).forEach(ReferenceCountUtil::release);
		return passedOn;
	}

	private void releaseEverythingPassedOn() {
		everythingPassedOn();
	}
}
