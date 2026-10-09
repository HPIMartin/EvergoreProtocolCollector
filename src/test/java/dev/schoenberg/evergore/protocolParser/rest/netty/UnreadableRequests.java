package dev.schoenberg.evergore.protocolParser.rest.netty;

import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;

public final class UnreadableRequests {
	private UnreadableRequests() {}

	public static UnreadableRequest standingInFor(HttpResponseStatus status, HttpHeaders carried) {
		return UnreadableRequest.standingInFor(status, carried);
	}
}
