package dev.schoenberg.evergore.protocolParser.rest.netty;

import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpVersion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.assertj.core.api.Assertions.assertThat;

class UnreadableHeadReplacerTest {
	private static final String USER_AGENT = "Firefox/128.0";
	private static final int LONGER_THAN_A_REQUEST_LINE_MAY_BE = 5000;
	private static final int LONGER_THAN_A_HEADER_BLOCK_MAY_BE = 9000;

	private final EmbeddedChannel channel = HeadChannel.withMaxHeaderSize(HeadChannel.DEFAULT_MAX_HEADER_SIZE);
	private final EmbeddedChannel readable = new EmbeddedChannel(new UnreadableHeadReplacer(HeadChannel.DEFAULT_MAX_HEADER_SIZE));
	private final EmbeddedChannel bare = new EmbeddedChannel(new HttpServerCodec());

	@AfterEach
	void closeChannels() {
		channel.finishAndReleaseAll();
		readable.finishAndReleaseAll();
		bare.finishAndReleaseAll();
	}

	@Test
	void passesARequestTheServerCanReadOnAsTheSameObject() {
		HttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/overview?token=secret", Unpooled.EMPTY_BUFFER);

		readable.writeInbound(request);
		Object passedOn = readable.readInbound();

		assertThat(passedOn).isSameAs(request);
	}

	@Test
	void answersARequestLineTheServerFindsTooLongWith413() {
		UnreadableRequest standIn = unreadableAfterReceiving("GET /" + "a".repeat(LONGER_THAN_A_REQUEST_LINE_MAY_BE) + " HTTP/1.1\r\nHost: localhost\r\n\r\n");

		assertThat(standIn.status().code()).isEqualTo(413);
	}

	@Test
	void answersAHeaderBlockTheServerFindsTooLongWith413() {
		UnreadableRequest standIn = unreadableAfterReceiving("GET / HTTP/1.1\r\nX-Padding: " + "a".repeat(LONGER_THAN_A_HEADER_BLOCK_MAY_BE) + "\r\n\r\n");

		assertThat(standIn.status().code()).isEqualTo(413);
	}

	@Test
	void neverCarriesTheTargetOrTheQueryOfTheRejectedRequest() {
		UnreadableRequest standIn = unreadableAfterReceiving(
				"GET /overview?token=secret HTTP/1.1\r\nUser-Agent: " + USER_AGENT + "\r\nX-Padding: " + "a".repeat(LONGER_THAN_A_HEADER_BLOCK_MAY_BE) + "\r\n\r\n");

		assertThat(standIn.uri()).isEqualTo("/bad-request");
		assertThat(standIn.toString()).doesNotContain("secret");
	}

	@Test
	void answersAMalformedRequestLineWith400() {
		UnreadableRequest standIn = unreadableAfterReceiving("GET\r\nHost: localhost\r\n\r\n");

		assertThat(standIn.status().code()).isEqualTo(400);
	}

	@Test
	void answersAnUnreadableContentLengthWith400() {
		UnreadableRequest standIn = unreadableAfterReceiving("GET / HTTP/1.1\r\nUser-Agent: " + USER_AGENT + "\r\nContent-Length: abc\r\n\r\n");

		assertThat(standIn.status().code()).isEqualTo(400);
		assertThat(standIn.headers().get("User-Agent")).isEqualTo(USER_AGENT);
	}

	@Test
	void standsInAsABodylessGetThatClosesTheConnection() {
		UnreadableRequest standIn = unreadableAfterReceiving("GET / HTTP/1.1\r\nUser-Agent: " + USER_AGENT
				+ "\r\nContent-Length: 5\r\nContent-Length: 6\r\nTransfer-Encoding: chunked\r\nExpect: 100-continue\r\nConnection: keep-alive\r\n\r\n");

		assertThat(standIn.method()).isEqualTo(HttpMethod.GET);
		assertThat(standIn.content().readableBytes()).isZero();
		assertThat(standIn.headers().names()).map(String::toLowerCase).containsExactlyInAnyOrder("user-agent", "connection");
		assertThat(standIn.headers().get("Connection")).isEqualTo("close");
	}

	@Test
	void passesOnAHeadTheClientCutOffByClosingTheConnectionAsItIs() {
		channel.writeInbound(Unpooled.copiedBuffer("GET / HTTP/1.1\r\nHost: localhost", US_ASCII));

		channel.close();
		Object passedOn = channel.readInbound();

		assertThat(passedOn).isInstanceOf(HttpRequest.class).isNotInstanceOf(UnreadableRequest.class);
	}

	@Test
	void theCodecHandsOverARequestOfItsOwnForARequestLineItCannotRead() {
		bare.writeInbound(Unpooled.copiedBuffer("GET /overview?token=secret" + "a".repeat(LONGER_THAN_A_REQUEST_LINE_MAY_BE) + " HTTP/1.1\r\n\r\n", US_ASCII));
		Object handedOver = bare.readInbound();

		assertThat(handedOver).isInstanceOfSatisfying(FullHttpRequest.class, request -> assertThat(request.uri()).isEqualTo("/bad-request"));
	}

	@Test
	void theCodecHandsOverTheRequestItWasReadingWhenAHeaderLineFails() {
		String head = "GET /overview HTTP/1.1\r\nHost: x\r\nUser-Agent: F\r\nX-Padding: " + "a".repeat(LONGER_THAN_A_HEADER_BLOCK_MAY_BE) + "\r\n\r\n";

		bare.writeInbound(Unpooled.copiedBuffer(head, US_ASCII));
		Object handedOver = bare.readInbound();

		assertThat(handedOver).isInstanceOfSatisfying(HttpRequest.class, request -> {
			assertThat(request.uri()).isEqualTo("/overview");
			assertThat(request.headers().names()).containsExactly("Host");
		});
		assertThat(handedOver).isNotInstanceOf(FullHttpRequest.class);
	}

	@Test
	void theCodecHandsOverTheHeaderBeforeALineWithoutAColonAlreadyRead() {
		bare.writeInbound(Unpooled.copiedBuffer("GET / HTTP/1.1\r\nUser-Agent: " + USER_AGENT + "\r\nNot a header line\r\n", US_ASCII));
		Object handedOver = bare.readInbound();

		assertThat(handedOver).isInstanceOfSatisfying(HttpRequest.class, request -> {
			assertThat(request.decoderResult().isFailure()).isTrue();
			assertThat(request.headers().get("User-Agent")).isEqualTo(USER_AGENT);
		});
	}

	private UnreadableRequest unreadableAfterReceiving(String received) {
		channel.writeInbound(Unpooled.copiedBuffer(received, US_ASCII));
		Object passedOn = channel.readInbound();

		assertThat(passedOn).isInstanceOf(UnreadableRequest.class);
		return (UnreadableRequest) passedOn;
	}
}
