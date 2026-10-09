package dev.schoenberg.evergore.protocolParser.rest.netty;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.DefaultHttpHeadersFactory;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

public final class UnreadableRequest extends DefaultFullHttpRequest {
	public static final String TARGET = "/bad-request";

	private static final Set<String> NOT_CARRIED = Set.of("content-length", "transfer-encoding", "connection", "expect");

	private final HttpResponseStatus status;

	private UnreadableRequest(HttpResponseStatus status, HttpHeaders headers) {
		super(HttpVersion.HTTP_1_1, HttpMethod.GET, TARGET, Unpooled.EMPTY_BUFFER, headers, DefaultHttpHeadersFactory.trailersFactory().newEmptyHeaders());
		this.status = status;
	}

	static UnreadableRequest standingInFor(HttpResponseStatus status, HttpHeaders carried) {
		HttpHeaders headers = new DefaultHttpHeaders();
		for (Map.Entry<String, String> header : carried) {
			if (!NOT_CARRIED.contains(header.getKey().toLowerCase(Locale.ROOT))) {
				headers.add(header.getKey(), header.getValue());
			}
		}
		headers.set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
		return new UnreadableRequest(status, headers);
	}

	public HttpResponseStatus status() {
		return status;
	}
}
