package dev.schoenberg.evergore.protocolParser.rest.filter;

import jakarta.inject.Singleton;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.HttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import org.reactivestreams.Publisher;

import dev.schoenberg.evergore.protocolParser.Logger;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.Login;
import dev.schoenberg.evergore.protocolParser.exceptions.AccessNotAllowed;

@Singleton
@Filter("/**")
public class AccessFilter implements HttpServerFilter {
	private final PathCanonicalizer canonicalizer;
	private final PublicPaths publicPaths;
	private final GuildToken guildToken;
	private final Logger logger;

	public AccessFilter(PathCanonicalizer canonicalizer, PublicPaths publicPaths, GuildToken guildToken, Logger logger) {
		this.canonicalizer = canonicalizer;
		this.publicPaths = publicPaths;
		this.guildToken = guildToken;
		this.logger = logger;
	}

	@Override
	public int getOrder() {
		return FilterOrder.ACCESS.position();
	}

	@Override
	public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {
		if (publicPaths.contains(canonicalizer.canonicalize(request.getPath())) || hasALogin(request) || guildToken.opens(request)) {
			return chain.proceed(request);
		}

		logger.info("Access rejected");
		throw new AccessNotAllowed();
	}

	private static boolean hasALogin(HttpRequest<?> request) {
		return request.getAttribute(LoginAttribute.NAME, Login.class).isPresent();
	}
}
