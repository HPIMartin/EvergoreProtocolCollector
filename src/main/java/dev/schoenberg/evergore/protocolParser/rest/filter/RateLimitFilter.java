package dev.schoenberg.evergore.protocolParser.rest.filter;

import jakarta.inject.Singleton;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.HttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import org.reactivestreams.Publisher;

import dev.schoenberg.evergore.protocolParser.exceptions.TooManyRequests;

@Singleton
@Filter("/**")
public class RateLimitFilter implements HttpServerFilter {
	private final RateLimitCounters counters;
	private final ClientIp clientIp;

	public RateLimitFilter(RateLimitCounters counters, ClientIp clientIp) {
		this.counters = counters;
		this.clientIp = clientIp;
	}

	@Override
	public int getOrder() {
		return FilterOrder.RATE_LIMIT.position();
	}

	@Override
	public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {
		String client = clientIp.of(request);

		if (counters.blocks(client)) {
			throw new TooManyRequests(client);
		}

		return chain.proceed(request);
	}
}
