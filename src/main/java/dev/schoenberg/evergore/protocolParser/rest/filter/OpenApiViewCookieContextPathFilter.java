package dev.schoenberg.evergore.protocolParser.rest.filter;

import java.net.*;
import java.nio.charset.*;

import org.reactivestreams.*;

import io.micronaut.context.annotation.*;
import io.micronaut.core.async.publisher.*;
import io.micronaut.http.*;
import io.micronaut.http.annotation.*;
import io.micronaut.http.cookie.*;
import io.micronaut.http.filter.*;

@Requires(property = "micronaut.server.context-path")
@Filter(methods = { HttpMethod.GET, HttpMethod.HEAD }, patterns = { "/**/rapidoc*", "/**/redoc*", "/**/swagger-ui*" })
public class OpenApiViewCookieContextPathFilter implements HttpServerFilter {
	private final Cookie contextPathCookie;

	OpenApiViewCookieContextPathFilter(@Value("${micronaut.server.context-path}") String contextPath) {
		this.contextPathCookie = Cookie.of("contextPath",
				URLEncoder.encode(contextPath, StandardCharsets.UTF_8) + ";max-age=2;samesite");
	}

	@Override
	public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {
		return Publishers.map(chain.proceed(request), response -> response.cookie(contextPathCookie));
	}

	@Override
	public int getOrder() {
		return 3;
	}
}