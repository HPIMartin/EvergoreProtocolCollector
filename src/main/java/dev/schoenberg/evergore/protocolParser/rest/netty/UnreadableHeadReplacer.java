package dev.schoenberg.evergore.protocolParser.rest.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.PrematureChannelClosureException;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.ReferenceCountUtil;

final class UnreadableHeadReplacer extends ChannelInboundHandlerAdapter {
	private final int maxHeaderSize;
	private ChannelHandlerContext context;
	private HttpResponseStatus rejectedStatus;
	private HttpHeaders rejectedHeaders;
	private boolean rejectedAtAHeaderLine;
	private HeaderRescue rescue;
	private boolean chunkedBody;
	private long bodyBytesLeft;
	private boolean discarding;

	UnreadableHeadReplacer(int maxHeaderSize) {
		this.maxHeaderSize = maxHeaderSize;
	}

	@Override
	public void handlerAdded(ChannelHandlerContext context) {
		this.context = context;
	}

	@Override
	public void channelRead(ChannelHandlerContext context, Object message) {
		if (message instanceof HttpRequest request && isUnreadable(request)) {
			rejectedStatus = statusTheServerAnswers(request.decoderResult().cause());
			rejectedHeaders = request.headers();
			rejectedAtAHeaderLine = !(request instanceof FullHttpRequest);
			ReferenceCountUtil.release(message);
			return;
		}

		trackTheBody(message);
		context.fireChannelRead(message);
	}

	int unslicedBytesWithin(int readable) {
		if (passesWholeReads()) {
			return readable;
		}
		return (int) Math.min(bodyBytesLeft, readable);
	}

	boolean passesWholeReads() {
		return discarding || chunkedBody;
	}

	void unslicedBytesPassed() {
		if (rejectedStatus == null || rescue != null) {
			return;
		}

		rescue = HeaderRescue.after(SliceEnd.END_OF_HEAD, maxHeaderSize);
		passOnIfComplete();
	}

	void idleBytesPassed() {
		if (rejectedStatus == null || rescue != null) {
			return;
		}

		rescue = rejectedAtAHeaderLine ? HeaderRescue.after(SliceEnd.END_OF_HEAD, maxHeaderSize) : HeaderRescue.beforeTheRequestLine(maxHeaderSize);
		passOnIfComplete();
	}

	boolean isCollecting() {
		return rescue != null;
	}

	void headForwarded(SliceEnd end, RecentLines lines) {
		if (rejectedStatus == null || rescue != null) {
			return;
		}

		rescue = HeaderRescue.after(end, maxHeaderSize);
		if (rejectedAtAHeaderLine) {
			rescueWhatTheDecoderStillHeld(end, lines);
		}
		passOnIfComplete();
	}

	void collect(byte[] slice) {
		rescue.feed(slice);
		passOnIfComplete();
	}

	private void passOnIfComplete() {
		if (!rescue.isComplete()) {
			return;
		}

		UnreadableRequest standIn = UnreadableRequest.standingInFor(rejectedStatus, rejectedHeaders, rescue.headers(), maxHeaderSize);
		forgetTheRejection();
		discarding = true;
		context.fireChannelRead(standIn);
	}

	private void rescueWhatTheDecoderStillHeld(SliceEnd end, RecentLines lines) {
		if (!rejectedHeaders.isEmpty()) {
			rescue.alsoRead(lines.previousLine());
		}
		if (end == SliceEnd.END_OF_LINE) {
			rescue.alsoRead(lines.currentLine());
		}
		if (end == SliceEnd.MID_LINE) {
			rescue.finishTheRejectedLine(lines.currentLine());
		}
	}

	private void trackTheBody(Object message) {
		if (message instanceof HttpRequest request && request.decoderResult().isSuccess()) {
			chunkedBody = HttpUtil.isTransferEncodingChunked(request);
			bodyBytesLeft = HttpUtil.getContentLength(request, 0L);
		}
		if (message instanceof HttpContent content) {
			bodyBytesLeft = Math.max(0, bodyBytesLeft - content.content().readableBytes());
		}
		if (message instanceof LastHttpContent last) {
			chunkedBody = false;
			bodyBytesLeft = 0;
			discarding = last.decoderResult().isFailure();
		}
	}

	private void forgetTheRejection() {
		rejectedStatus = null;
		rejectedHeaders = null;
		rejectedAtAHeaderLine = false;
		rescue = null;
	}

	private static boolean isUnreadable(HttpRequest request) {
		return request.decoderResult().isFailure() && !(request.decoderResult().cause() instanceof PrematureChannelClosureException);
	}

	private static HttpResponseStatus statusTheServerAnswers(Throwable cause) {
		return cause instanceof TooLongFrameException ? HttpResponseStatus.REQUEST_ENTITY_TOO_LARGE : HttpResponseStatus.BAD_REQUEST;
	}
}
