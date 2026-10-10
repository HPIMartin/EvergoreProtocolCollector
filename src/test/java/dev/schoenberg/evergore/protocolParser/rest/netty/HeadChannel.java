package dev.schoenberg.evergore.protocolParser.rest.netty;

import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.HttpServerCodec;

import static java.nio.charset.StandardCharsets.US_ASCII;

final class HeadChannel {
	static final int DEFAULT_MAX_HEADER_SIZE = 8192;

	private HeadChannel() {}

	static EmbeddedChannel withMaxHeaderSize(int maxHeaderSize) {
		EmbeddedChannel channel = new EmbeddedChannel();
		channel.pipeline().addLast(UnreadableHeadCustomizer.CODEC, new HttpServerCodec());
		UnreadableHeadCustomizer.install(channel.pipeline(), maxHeaderSize);
		return channel;
	}

	static void receive(EmbeddedChannel channel, String received) {
		channel.writeInbound(Unpooled.copiedBuffer(received, US_ASCII));
	}
}
