package dev.schoenberg.evergore.protocolParser.rest.filter;

import jakarta.inject.Singleton;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.HttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import org.reactivestreams.Publisher;

import dev.schoenberg.evergore.protocolParser.businessLogic.account.Login;
import dev.schoenberg.evergore.protocolParser.exceptions.TooManyRequests;

@Singleton
@Filter("/**")
public class RateLimitFilter implements HttpServerFilter {
	private final RateLimitCounters counters;
	private final AnonymousBuckets anonymousBuckets;
	private final ClientIp clientIp;

	public RateLimitFilter(RateLimitCounters counters, AnonymousBuckets anonymousBuckets, ClientIp clientIp) {
		this.anonymousBuckets = anonymousBuckets;
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

		if (turnsAway(request, client)) {
			throw new TooManyRequests(client);
		}

		return chain.proceed(request);
	}

	private boolean turnsAway(HttpRequest<?> request, String client) {
		if (request.getAttribute(LoginAttribute.NAME, Login.class).isPresent()) {
			return counters.blocks(client);
		}

		return !anonymousBuckets.admits(client);
	}
}
