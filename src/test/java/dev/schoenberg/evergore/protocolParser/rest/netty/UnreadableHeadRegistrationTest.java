package dev.schoenberg.evergore.protocolParser.rest.netty;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.micronaut.context.ApplicationContext;
import io.micronaut.http.server.netty.NettyServerCustomizer;
import io.micronaut.http.server.netty.NettyServerCustomizer.ChannelRole;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.HttpServerCodec;
import org.junit.jupiter.api.Test;

import static dev.schoenberg.evergore.protocolParser.ThrowawayDatabaseFactory.THROWAWAY_DATABASE_PATH;
import static dev.schoenberg.evergore.protocolParser.rest.netty.HeadChannel.receive;
import static org.assertj.core.api.Assertions.assertThat;

class UnreadableHeadRegistrationTest {
	private static final String THROWAWAY_DB_PATH = "build/tmp/test/unreadableHeadRegistrationTest.sqlite";
	private static final int SMALL_HEADER_BUDGET = 64;

	@Test
	void registersACustomizerWhoseHeaderBudgetIsTheMaxHeaderSizeOfTheServer() {
		Map<String, Object> smallMaxHeaderSize = Map
				.of("micronaut.server.netty.max-header-size", SMALL_HEADER_BUDGET, "micronaut.server.port", "-1", THROWAWAY_DATABASE_PATH, THROWAWAY_DB_PATH);
		List<NettyServerCustomizer> registered = new ArrayList<>();
		EmbeddedChannel channel = new EmbeddedChannel();
		channel.pipeline().addLast(UnreadableHeadCustomizer.CODEC, new HttpServerCodec());

		try (ApplicationContext context = ApplicationContext.run(smallMaxHeaderSize, "test")) {
			context.getBean(UnreadableHeadRegistration.class).register(registered::add);
			registered.getFirst().specializeForChannel(channel, ChannelRole.CONNECTION).onStreamPipelineBuilt();
			receive(channel, "GET /" + "a".repeat(5000) + " HTTP/1.1\r\nUser-Agent: Firefox/128.0\r\nX-Padding: " + "a".repeat(100) + "\r\nX-Late: 1\r\n\r\n");
		}
		UnreadableRequest standIn = (UnreadableRequest) channel.readInbound();

		assertThat(registered).hasSize(1);
		assertThat(standIn.headers().get("User-Agent")).isEqualTo("Firefox/128.0");
		assertThat(standIn.headers().contains("X-Late")).isFalse();
		channel.finishAndReleaseAll();
	}
}
