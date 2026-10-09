package dev.schoenberg.evergore.protocolParser.rest.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.PrematureChannelClosureException;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.ReferenceCountUtil;

public class UnreadableHeadReplacer extends ChannelInboundHandlerAdapter {
	@Override
	public void channelRead(ChannelHandlerContext context, Object message) {
		if (message instanceof HttpRequest request && isUnreadable(request)) {
			UnreadableRequest standIn = new UnreadableRequest(statusTheServerAnswers(request.decoderResult().cause()), request.headers());
			ReferenceCountUtil.release(message);
			context.fireChannelRead(standIn);
			return;
		}

		context.fireChannelRead(message);
	}

	private static boolean isUnreadable(HttpRequest request) {
		return request.decoderResult().isFailure() && !(request.decoderResult().cause() instanceof PrematureChannelClosureException);
	}

	private static HttpResponseStatus statusTheServerAnswers(Throwable cause) {
		return cause instanceof TooLongFrameException ? HttpResponseStatus.REQUEST_ENTITY_TOO_LARGE : HttpResponseStatus.BAD_REQUEST;
	}
}
