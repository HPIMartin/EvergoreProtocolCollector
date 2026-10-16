package dev.schoenberg.evergore.protocolParser.rest.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

final class HeadLineSlicer extends ChannelInboundHandlerAdapter {
	private static final byte LINE_FEED = '\n';

	private final UnreadableHeadReplacer replacer;
	private final RecentLines recentLines;
	private boolean atHeadStart = true;
	private boolean betweenMessages;

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
				boolean wholeRead = replacer.passesWholeReads();
				int unsliced = replacer.unslicedBytesWithin(bytes.readableBytes());
				if (unsliced > 0) {
					context.fireChannelRead(bytes.readRetainedSlice(unsliced));
					replacer.unslicedBytesPassed();
					atHeadStart = !wholeRead;
					betweenMessages = true;
					continue;
				}
				int idle = atHeadStart && !replacer.isCollecting() ? lengthOfIdleBytes(bytes) : 0;
				if (idle > 0) {
					context.fireChannelRead(bytes.readRetainedSlice(idle));
					replacer.idleBytesPassed();
					continue;
				}
				pass(context, bytes.readRetainedSlice(lengthOfNextSlice(bytes)));
			}
		} finally {
			bytes.release();
		}
	}

	private void pass(ChannelHandlerContext context, ByteBuf slice) {
		SliceEnd end = recentLines.append(slice);
		boolean idleLine = end == SliceEnd.END_OF_LINE && recentLines.currentLineIsIdle();
		if (end == SliceEnd.END_OF_LINE && !idleLine) {
			betweenMessages = false;
		}
		atHeadStart = end == SliceEnd.END_OF_HEAD || (idleLine && betweenMessages);
		if (replacer.isCollecting()) {
			byte[] collected = ByteBufUtil.getBytes(slice);
			slice.release();
			replacer.collect(collected);
			return;
		}

		context.fireChannelRead(slice);
		replacer.headForwarded(end, recentLines);
	}

	private static int lengthOfIdleBytes(ByteBuf bytes) {
		int firstOfTheHead = bytes.forEachByte(RecentLines::isIdle);
		return firstOfTheHead < 0 ? bytes.readableBytes() : firstOfTheHead - bytes.readerIndex();
	}

	private static int lengthOfNextSlice(ByteBuf bytes) {
		int lineFeed = bytes.indexOf(bytes.readerIndex(), bytes.writerIndex(), LINE_FEED);
		return lineFeed < 0 ? bytes.readableBytes() : lineFeed - bytes.readerIndex() + 1;
	}
}
