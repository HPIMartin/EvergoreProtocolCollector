package dev.schoenberg.evergore.protocolParser.rest.filter;

import org.reactivestreams.*;

import dev.schoenberg.evergore.protocolParser.*;
import io.micronaut.core.order.*;
import io.micronaut.http.*;
import io.micronaut.http.annotation.*;
import io.micronaut.http.filter.*;
import jakarta.inject.*;

@Singleton
@Filter("/**")
public class BrowserLoggingFilter implements HttpServerFilter {
	private final Logger logger;

	public BrowserLoggingFilter(Logger logger) {
		this.logger = logger;
	}

	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE;
	}

	@Override
	public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {
		String userAgent = request.getHeaders().get("user-agent");
		String clientIp = request.getRemoteAddress().getAddress().getHostAddress();
		logger.info("Client IP: " + clientIp + " Agent: " + userAgent);
		return chain.proceed(request);
	}
}
