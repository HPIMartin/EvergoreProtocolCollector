package dev.schoenberg.evergore.protocolParser.rest.filter;

import jakarta.inject.*;

import io.micronaut.http.*;
import io.micronaut.http.annotation.*;
import io.micronaut.http.filter.*;
import org.reactivestreams.*;

import dev.schoenberg.evergore.protocolParser.*;
import dev.schoenberg.evergore.protocolParser.exceptions.*;
import dev.schoenberg.evergore.protocolParser.rest.controller.*;

@Singleton
@Filter("/**")
public class TokenValidationFilter implements HttpServerFilter {
	private static final String TOKEN_PARAMETER_NAME = "token";
	private static final String VALID_TOKEN = "secret_token";

	private final Logger logger;

	public TokenValidationFilter(Logger logger) {
		this.logger = logger;
	}

	@Override
	public int getOrder() {
		return 2;
	}

	@Override
	public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {
		if (isPublicEndpoint(request)) {
			return chain.proceed(request);
		}

		String token = request.getParameters().get(TOKEN_PARAMETER_NAME, String.class).orElseThrow(this::reject);
		if (!token.equals(VALID_TOKEN)) {
			throw reject();
		}

		return chain.proceed(request);
	}

	private boolean isPublicEndpoint(HttpRequest<?> request) {
		String path = request.getPath();
		return path.equals(FaviconController.PATH) || path.equals("/health") || path.startsWith("/health/");
	}

	private AccessNotAllowed reject() {
		logger.info("Access rejected");
		return new AccessNotAllowed();
	}
}
