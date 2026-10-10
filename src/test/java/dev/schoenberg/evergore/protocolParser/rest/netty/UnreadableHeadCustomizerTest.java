package dev.schoenberg.evergore.protocolParser.rest.netty;

import java.util.List;

import io.micronaut.http.server.netty.NettyServerCustomizer;
import io.micronaut.http.server.netty.NettyServerCustomizer.ChannelRole;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.HttpServerCodec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static dev.schoenberg.evergore.protocolParser.rest.netty.HeadChannel.receive;
import static org.assertj.core.api.Assertions.assertThat;

class UnreadableHeadCustomizerTest {
	private static final int SMALL_HEADER_BUDGET = 64;

	private final EmbeddedChannel channel = new EmbeddedChannel();
	private final UnreadableHeadCustomizer tested = new UnreadableHeadCustomizer(() -> SMALL_HEADER_BUDGET);

	@AfterEach
	void closeChannel() {
		channel.finishAndReleaseAll();
	}

	@Test
	void takesTheHeaderBudgetFromTheServerConfigurationWhenTheConnectionPipelineIsBuilt() {
		channel.pipeline().addLast(UnreadableHeadCustomizer.CODEC, new HttpServerCodec());
		NettyServerCustomizer forTheConnection = tested.specializeForChannel(channel, ChannelRole.CONNECTION);

		forTheConnection.onStreamPipelineBuilt();
		receive(channel, "GET /" + "a".repeat(5000) + " HTTP/1.1\r\nUser-Agent: Firefox/128.0\r\nX-Padding: " + "a".repeat(100) + "\r\nX-Late: 1\r\n\r\n");
		UnreadableRequest standIn = (UnreadableRequest) channel.readInbound();

		assertThat(standIn.headers().get("User-Agent")).isEqualTo("Firefox/128.0");
		assertThat(standIn.headers().contains("X-Late")).isFalse();
	}

	@Test
	void installsOnceWhenTheStreamPipelineIsBuiltTwice() {
		channel.pipeline().addLast(UnreadableHeadCustomizer.CODEC, new HttpServerCodec());
		NettyServerCustomizer forTheConnection = tested.specializeForChannel(channel, ChannelRole.CONNECTION);
		forTheConnection.onStreamPipelineBuilt();
		List<String> afterTheFirstCall = channel.pipeline().names();

		forTheConnection.onStreamPipelineBuilt();

		assertThat(channel.pipeline().names()).isEqualTo(afterTheFirstCall);
	}

	@Test
	void leavesAConnectionWithoutAnHttpCodecAlone() {
		List<String> before = channel.pipeline().names();
		NettyServerCustomizer forTheConnection = tested.specializeForChannel(channel, ChannelRole.CONNECTION);

		forTheConnection.onStreamPipelineBuilt();

		assertThat(channel.pipeline().names()).isEqualTo(before);
	}

	@Test
	void leavesTheStreamOfARequestAlone() {
		channel.pipeline().addLast(UnreadableHeadCustomizer.CODEC, new HttpServerCodec());
		List<String> before = channel.pipeline().names();
		NettyServerCustomizer forTheStream = tested.specializeForChannel(channel, ChannelRole.REQUEST_STREAM);

		forTheStream.onStreamPipelineBuilt();

		assertThat(channel.pipeline().names()).isEqualTo(before);
	}
}
