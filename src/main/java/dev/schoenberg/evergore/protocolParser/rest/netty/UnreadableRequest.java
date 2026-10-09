package dev.schoenberg.evergore.protocolParser.rest.netty;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValidationUtil;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

public final class UnreadableRequest extends DefaultFullHttpRequest {
	public static final String TARGET = "/bad-request";

	private static final int NONE = -1;
	private static final int UNBOUNDED = Integer.MAX_VALUE;
	private static final int SEPARATOR_AND_LINE_END_SIZE = 4;
	private static final Set<String> NOT_CARRIED = Set.of("content-length", "transfer-encoding", "connection", "expect");

	private final HttpResponseStatus status;

	public UnreadableRequest(HttpResponseStatus status, HttpHeaders carried) {
		this(status, carried, List.of(), UNBOUNDED);
	}

	UnreadableRequest(HttpResponseStatus status, HttpHeaders carried, List<RescuedHeader> rescued, int budget) {
		super(HttpVersion.HTTP_1_1, HttpMethod.GET, TARGET, Unpooled.EMPTY_BUFFER);
		this.status = status;
		List<RescuedHeader> readable = new ArrayList<>();
		for (Map.Entry<String, String> header : carried) {
			collectIfReadable(readable, new RescuedHeader(header.getKey(), header.getValue()));
		}
		rescued.stream().filter(header -> !isAmong(readable, header)).forEach(header -> collectIfReadable(readable, header));
		dropTheLargestWhileOverTheBudget(readable, budget);
		readable.forEach(header -> headers().add(header.name(), header.value()));
		headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
	}

	public HttpResponseStatus status() {
		return status;
	}

	private static void collectIfReadable(List<RescuedHeader> readable, RescuedHeader header) {
		if (!NOT_CARRIED.contains(header.name().toLowerCase(Locale.ROOT)) && isReadable(header)) {
			readable.add(header);
		}
	}

	private static boolean isAmong(List<RescuedHeader> headers, RescuedHeader header) {
		return headers.stream().anyMatch(other -> other.name().equalsIgnoreCase(header.name()) && other.value().equals(header.value()));
	}

	private static void dropTheLargestWhileOverTheBudget(List<RescuedHeader> headers, int budget) {
		long used = sizeOf(HttpHeaderNames.CONNECTION.toString(), HttpHeaderValues.CLOSE.toString());
		used += headers.stream().mapToLong(UnreadableRequest::sizeOf).sum();
		while (used > budget && !headers.isEmpty()) {
			RescuedHeader largest = headers.stream().max(Comparator.comparingInt(UnreadableRequest::sizeOf)).orElseThrow();
			headers.remove(largest);
			used -= sizeOf(largest);
		}
	}

	private static int sizeOf(RescuedHeader header) {
		return sizeOf(header.name(), header.value());
	}

	private static int sizeOf(String name, String value) {
		return name.length() + value.length() + SEPARATOR_AND_LINE_END_SIZE;
	}

	private static boolean isReadable(RescuedHeader header) {
		return HttpHeaderValidationUtil.validateToken(header.name()) == NONE && HttpHeaderValidationUtil.validateValidHeaderValue(header.value()) == NONE;
	}
}
