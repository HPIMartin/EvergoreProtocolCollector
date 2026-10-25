package dev.schoenberg.evergore.protocolParser.rest.filter;

import jakarta.inject.Singleton;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.HttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import org.reactivestreams.Publisher;

import dev.schoenberg.evergore.protocolParser.application.ValidLogins;
import dev.schoenberg.evergore.protocolParser.rest.controller.api.SessionCookie;

@Singleton
@Filter("/**")
public class LoginReadingFilter implements HttpServerFilter {
	private final ValidLogins validLogins;

	public LoginReadingFilter(ValidLogins validLogins) {
		this.validLogins = validLogins;
	}

	@Override
	public int getOrder() {
		return FilterOrder.LOGIN_READING.position();
	}

	@Override
	public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {
		request
				.getCookies()
				.findCookie(SessionCookie.NAME)
				.flatMap(cookie -> validLogins.behind(cookie.getValue()))
				.ifPresent(login -> request.setAttribute(LoginAttribute.NAME, login));

		return chain.proceed(request);
	}
}
