package dev.schoenberg.evergore.protocolParser.rest.netty;

import io.micronaut.http.server.netty.NettyServerCustomizer;
import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;

public final class UnreadableHeadCustomizer implements NettyServerCustomizer {
	static final String CODEC = "http-server-codec";
	static final String REPLACER = "unreadable-head-replacer";

	@Override
	public NettyServerCustomizer specializeForChannel(Channel channel, ChannelRole role) {
		return role == ChannelRole.CONNECTION ? new ForConnection(channel.pipeline()) : this;
	}

	static void install(ChannelPipeline pipeline) {
		pipeline.addAfter(CODEC, REPLACER, new UnreadableHeadReplacer());
	}

	private static final class ForConnection implements NettyServerCustomizer {
		private final ChannelPipeline pipeline;

		ForConnection(ChannelPipeline pipeline) {
			this.pipeline = pipeline;
		}

		@Override
		public void onStreamPipelineBuilt() {
			if (pipeline.get(CODEC) != null) {
				install(pipeline);
			}
		}
	}
}
