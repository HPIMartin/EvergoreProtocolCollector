package dev.schoenberg.evergore.protocolParser.rest.filter;

import jakarta.inject.Singleton;

import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.HttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import io.micronaut.http.server.netty.NettyHttpRequest;
import org.reactivestreams.Publisher;

import dev.schoenberg.evergore.protocolParser.rest.netty.UnreadableRequest;

@Singleton
@Filter("/**")
public class UnreadableRequestFilter implements HttpServerFilter {
	@Override
	public int getOrder() {
		return FilterOrder.UNREADABLE_REQUEST.position();
	}

	@Override
	public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {
		if (request instanceof NettyHttpRequest<?> served && served.getNativeRequest() instanceof UnreadableRequest unreadable) {
			return Publishers.just(HttpResponse.status(HttpStatus.valueOf(unreadable.status().code())));
		}

		return chain.proceed(request);
	}
}
