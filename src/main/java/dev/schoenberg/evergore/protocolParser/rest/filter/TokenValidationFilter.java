package dev.schoenberg.evergore.protocolParser.rest.filter;

import java.security.MessageDigest;

import jakarta.inject.Singleton;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.HttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import org.reactivestreams.Publisher;

import dev.schoenberg.evergore.protocolParser.Logger;
import dev.schoenberg.evergore.protocolParser.exceptions.AccessNotAllowed;
import dev.schoenberg.evergore.protocolParser.helper.config.SecurityConfiguration;

import static java.nio.charset.StandardCharsets.UTF_8;

@Singleton
@Filter("/**")
public class TokenValidationFilter implements HttpServerFilter {
	private static final String TOKEN_PARAMETER_NAME = "token";

	private final SecurityConfiguration securityConfiguration;
	private final PathCanonicalizer canonicalizer;
	private final PublicPaths publicPaths;
	private final Logger logger;

	public TokenValidationFilter(SecurityConfiguration securityConfiguration, PathCanonicalizer canonicalizer, PublicPaths publicPaths, Logger logger) {
		this.securityConfiguration = securityConfiguration;
		this.canonicalizer = canonicalizer;
		this.publicPaths = publicPaths;
		this.logger = logger;
	}

	@Override
	public int getOrder() {
		return FilterOrder.TOKEN_VALIDATION.position();
	}

	@Override
	public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {
		if (publicPaths.contains(canonicalizer.canonicalize(request.getPath()))) {
			return chain.proceed(request);
		}

		String token = request.getParameters().get(TOKEN_PARAMETER_NAME, String.class).orElseThrow(this::reject);
		if (!matchesApiToken(token)) {
			throw reject();
		}

		return chain.proceed(request);
	}

	private boolean matchesApiToken(String presented) {
		String expected = securityConfiguration.apiToken();
		if (expected == null || expected.isBlank()) {
			return false;
		}
		return MessageDigest.isEqual(presented.getBytes(UTF_8), expected.getBytes(UTF_8));
	}

	private AccessNotAllowed reject() {
		logger.info("Access rejected");
		return new AccessNotAllowed();
	}
}
