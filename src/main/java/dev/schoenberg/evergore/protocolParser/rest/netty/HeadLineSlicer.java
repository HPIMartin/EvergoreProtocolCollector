package dev.schoenberg.evergore.protocolParser.rest.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

final class HeadLineSlicer extends ChannelInboundHandlerAdapter {
	private static final byte LINE_FEED = '\n';

	private final UnreadableHeadReplacer replacer;
	private final RecentLines recentLines;

	HeadLineSlicer(UnreadableHeadReplacer replacer, int maxHeaderSize) {
		this.replacer = replacer;
		this.recentLines = new RecentLines(maxHeaderSize);
	}

	@Override
	public void channelRead(ChannelHandlerContext context, Object message) {
		if (!(message instanceof ByteBuf bytes)) {
			context.fireChannelRead(message);
			return;
		}

		try {
			while (bytes.isReadable()) {
				pass(context, bytes.readRetainedSlice(lengthOfNextSlice(bytes)));
			}
		} finally {
			bytes.release();
		}
	}

	private void pass(ChannelHandlerContext context, ByteBuf slice) {
		SliceEnd end = recentLines.append(slice);
		if (replacer.isCollecting()) {
			byte[] collected = ByteBufUtil.getBytes(slice);
			slice.release();
			replacer.collect(collected);
			return;
		}

		context.fireChannelRead(slice);
		replacer.headForwarded(end, recentLines);
	}

	private static int lengthOfNextSlice(ByteBuf bytes) {
		int lineFeed = bytes.indexOf(bytes.readerIndex(), bytes.writerIndex(), LINE_FEED);
		return lineFeed < 0 ? bytes.readableBytes() : lineFeed - bytes.readerIndex() + 1;
	}
}
