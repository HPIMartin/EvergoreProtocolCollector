package dev.schoenberg.evergore.protocolParser.rest.filter;

import org.reactivestreams.*;

import dev.schoenberg.evergore.protocolParser.*;
import dev.schoenberg.evergore.protocolParser.exceptions.*;
import io.micronaut.http.*;
import io.micronaut.http.annotation.*;
import io.micronaut.http.filter.*;
import jakarta.inject.*;

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
	public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {
		String token = request.getParameters().get(TOKEN_PARAMETER_NAME, String.class).orElseThrow(this::reject);

		if (!token.equals(VALID_TOKEN)) {
			throw reject();
		}

		return chain.proceed(request);
	}

	private AccessNotAllowed reject() {
		logger.info("Access rejected");
		return new AccessNotAllowed();
	}
}
