package dev.schoenberg.evergore.protocolParser.rest.filter;

import java.util.Optional;

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
	private final LoginPots loginPots;
	private final AnonymousBuckets anonymousBuckets;
	private final ClientIp clientIp;
	private final PathCanonicalizer canonicalizer;

	public RateLimitFilter(LoginPots loginPots, AnonymousBuckets anonymousBuckets, ClientIp clientIp, PathCanonicalizer canonicalizer) {
		this.loginPots = loginPots;
		this.anonymousBuckets = anonymousBuckets;
		this.clientIp = clientIp;
		this.canonicalizer = canonicalizer;
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
		Optional<Login> login = request.getAttribute(LoginAttribute.NAME, Login.class);
		if (login.isPresent() && !LoginPaths.isOfTheLogin(canonicalizer.canonicalize(request.getPath()))) {
			return loginPots.turnsAway(client, login.get().accountName());
		}

		return loginPots.turnsAwayAddress(client) || !anonymousBuckets.admits(client);
	}
}
