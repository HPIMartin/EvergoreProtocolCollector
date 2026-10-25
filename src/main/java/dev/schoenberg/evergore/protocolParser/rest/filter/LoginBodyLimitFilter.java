package dev.schoenberg.evergore.protocolParser.rest.filter;

import jakarta.inject.Singleton;

import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.http.HttpMethod;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.HttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import org.reactivestreams.Publisher;

import dev.schoenberg.evergore.protocolParser.rest.controller.api.LoginController;
import dev.schoenberg.evergore.protocolParser.rest.controller.api.PasswordController;

@Singleton
@Filter("/**")
public class LoginBodyLimitFilter implements HttpServerFilter {
	private static final long LIMIT_BYTES = 4096;
	private static final String TRANSFER_ENCODING = "Transfer-Encoding";
	private static final String CONTENT_ENCODING = "Content-Encoding";

	private final PathCanonicalizer canonicalizer;

	public LoginBodyLimitFilter(PathCanonicalizer canonicalizer) {
		this.canonicalizer = canonicalizer;
	}

	@Override
	public int getOrder() {
		return FilterOrder.LOGIN_BODY_LIMIT.position();
	}

	@Override
	public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {
		if (takesAPasswordAt(canonicalizer.canonicalize(request.getPath())) && !hasADeclaredLengthWithinTheLimit(request)) {
			return Publishers.just(HttpResponse.status(HttpStatus.REQUEST_ENTITY_TOO_LARGE));
		}

		return chain.proceed(request);
	}

	private static boolean takesAPasswordAt(String canonicalPath) {
		return canonicalPath.equals(LoginController.PATH) || canonicalPath.startsWith(LoginController.PATH + "/") || canonicalPath.equals(PasswordController.PATH);
	}

	private static boolean hasADeclaredLengthWithinTheLimit(HttpRequest<?> request) {
		boolean undeclared = request.getContentLength() < 0 && HttpMethod.permitsRequestBody(request.getMethod());

		return request.getContentLength() <= LIMIT_BYTES && !undeclared && !isEncoded(request);
	}

	private static boolean isEncoded(HttpRequest<?> request) {
		return namesAnEncoding(request, TRANSFER_ENCODING) || namesAnEncoding(request, CONTENT_ENCODING);
	}

	private static boolean namesAnEncoding(HttpRequest<?> request, String header) {
		return request.getHeaders().getAll(header).stream().anyMatch(line -> !line.isBlank());
	}
}
