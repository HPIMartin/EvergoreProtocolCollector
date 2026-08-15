package dev.schoenberg.evergore.protocolParser.rest.filter;

import jakarta.inject.Singleton;

import io.micronaut.http.HttpRequest;

@Singleton
class ClientIp {
	String of(HttpRequest<?> request) {
		return request.getRemoteAddress().getAddress().getHostAddress();
	}
}
