package dev.schoenberg.evergore.protocolParser.rest.netty;

import java.util.function.IntSupplier;

import io.micronaut.http.server.netty.NettyServerCustomizer;
import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;

public final class UnreadableHeadCustomizer implements NettyServerCustomizer {
	static final String CODEC = "http-server-codec";
	static final String SLICER = "head-line-slicer";
	static final String REPLACER = "unreadable-head-replacer";

	private final IntSupplier maxHeaderSize;

	public UnreadableHeadCustomizer(IntSupplier maxHeaderSize) {
		this.maxHeaderSize = maxHeaderSize;
	}

	@Override
	public NettyServerCustomizer specializeForChannel(Channel channel, ChannelRole role) {
		return role == ChannelRole.CONNECTION ? new ForConnection(channel.pipeline(), maxHeaderSize) : this;
	}

	static void install(ChannelPipeline pipeline, int maxHeaderSize) {
		UnreadableHeadReplacer replacer = new UnreadableHeadReplacer(maxHeaderSize);
		pipeline.addBefore(CODEC, SLICER, new HeadLineSlicer(replacer, maxHeaderSize));
		pipeline.addAfter(CODEC, REPLACER, replacer);
	}

	private static final class ForConnection implements NettyServerCustomizer {
		private final ChannelPipeline pipeline;
		private final IntSupplier maxHeaderSize;

		ForConnection(ChannelPipeline pipeline, IntSupplier maxHeaderSize) {
			this.pipeline = pipeline;
			this.maxHeaderSize = maxHeaderSize;
		}

		@Override
		public void onStreamPipelineBuilt() {
			if (pipeline.get(CODEC) != null && pipeline.get(SLICER) == null) {
				install(pipeline, maxHeaderSize.getAsInt());
			}
		}
	}
}
