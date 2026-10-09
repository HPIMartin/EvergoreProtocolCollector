package dev.schoenberg.evergore.protocolParser.rest.netty;

import java.util.List;

import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;

public final class UnreadableRequests {
	private static final int UNBOUNDED = Integer.MAX_VALUE;

	private UnreadableRequests() {}

	public static UnreadableRequest standingInFor(HttpResponseStatus status, HttpHeaders carried) {
		return UnreadableRequest.standingInFor(status, carried, List.of(), UNBOUNDED);
	}
}
