package dev.schoenberg.evergore.protocolParser.rest.filter;

import jakarta.inject.Singleton;

import io.micronaut.context.annotation.Replaces;
import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpRequest;

import dev.schoenberg.evergore.protocolParser.acceptance.service.AcceptanceEnvironment;

@Singleton
@Replaces(ClientIp.class)
@Requires(env = AcceptanceEnvironment.NAME)
public class AcceptanceClientIp extends ClientIp {
	public static final String HEADER = "X-Acceptance-Client-Address";

	@Override
	String of(HttpRequest<?> request) {
		String address = request.getHeaders().get(HEADER);
		return address == null ? super.of(request) : address;
	}
}
