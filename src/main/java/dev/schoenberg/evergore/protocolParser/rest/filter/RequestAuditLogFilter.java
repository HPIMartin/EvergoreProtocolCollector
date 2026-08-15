package dev.schoenberg.evergore.protocolParser.rest.filter;

import jakarta.inject.Singleton;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.HttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import org.reactivestreams.Publisher;

import dev.schoenberg.evergore.protocolParser.Logger;

@Singleton
@Filter("/**")
public class RequestAuditLogFilter implements HttpServerFilter {
	private static final String USER_AGENT_HEADER = "user-agent";

	private final ClientIp clientIp;
	private final Logger logger;

	public RequestAuditLogFilter(ClientIp clientIp, Logger logger) {
		this.clientIp = clientIp;
		this.logger = logger;
	}

	@Override
	public int getOrder() {
		return FilterOrder.REQUEST_AUDIT_LOG.position();
	}

	@Override
	public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {
		logger.info("Client IP: " + clientIp.of(request) + " Agent: " + request.getHeaders().get(USER_AGENT_HEADER));

		return chain.proceed(request);
	}
}
