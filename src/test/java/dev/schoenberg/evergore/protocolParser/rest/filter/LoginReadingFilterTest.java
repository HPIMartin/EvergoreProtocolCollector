package dev.schoenberg.evergore.protocolParser.rest.filter;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.cookie.Cookie;
import io.micronaut.http.filter.ServerFilterChain;
import io.micronaut.http.simple.SimpleHttpRequestFactory;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;

import dev.schoenberg.evergore.protocolParser.application.LogOut;
import dev.schoenberg.evergore.protocolParser.application.LoginGenerations;
import dev.schoenberg.evergore.protocolParser.application.SignedLogins;
import dev.schoenberg.evergore.protocolParser.application.ValidLogins;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.Login;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.LoginSource;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.Role;

import static org.assertj.core.api.Assertions.assertThat;

class LoginReadingFilterTest {
	private static final Instant MADE = Instant.parse("2026-01-05T10:00:00Z");
	private static final String SESSION_COOKIE = "epc_session";
	private static final String SIGNED_VALUE = "signed-login-value";
	private static final Login AURORA = new Login("Aurora", Role.MEMBER, 0, LoginSource.ACCOUNT, "id", MADE, MADE.plus(Login.LIFETIME));

	private final Publisher<MutableHttpResponse<?>> passedThrough = _ -> {};
	private final List<HttpRequest<?>> reachedTheChain = new ArrayList<>();
	private final ServerFilterChain chain = request -> {
		reachedTheChain.add(request);
		return passedThrough;
	};
	private final LoginGenerations generations = new LoginGenerations();
	private final LoginReadingFilter tested = new LoginReadingFilter(new ValidLogins(new SignsOnlyOneValue(), generations, new LogOut(Clock.systemUTC())));

	@Test
	void putsTheLoginOfTheSessionCookieOnTheRequest() {
		MutableHttpRequest<?> request = requestWithCookie(SESSION_COOKIE, SIGNED_VALUE);

		tested.doFilter(request, chain);

		assertThat(loginOn(request)).contains(AURORA);
	}

	@Test
	void putsNoLoginOnARequestWithoutACookie() {
		MutableHttpRequest<?> request = new SimpleHttpRequestFactory().get("/overview");

		tested.doFilter(request, chain);

		assertThat(loginOn(request)).isEmpty();
	}

	@Test
	void putsNoLoginOnARequestWhoseCookieValueTheServiceDidNotSign() {
		MutableHttpRequest<?> request = requestWithCookie(SESSION_COOKIE, "forged");

		tested.doFilter(request, chain);

		assertThat(loginOn(request)).isEmpty();
	}

	@Test
	void putsNoLoginOnARequestWhoseSignedValueIsOfTheCookieOfAnotherName() {
		MutableHttpRequest<?> request = requestWithCookie("other", SIGNED_VALUE);

		tested.doFilter(request, chain);

		assertThat(loginOn(request)).isEmpty();
	}

	@Test
	void putsNoLoginOnARequestWhoseAccountHasMovedOnToANewGeneration() {
		generations.bumpWhileAt("aurora", 0, () -> {});
		MutableHttpRequest<?> request = requestWithCookie(SESSION_COOKIE, SIGNED_VALUE);

		tested.doFilter(request, chain);

		assertThat(loginOn(request)).isEmpty();
	}

	@Test
	void passesTheRequestOnWhateverItsLogin() {
		MutableHttpRequest<?> request = requestWithCookie(SESSION_COOKIE, "forged");

		Publisher<MutableHttpResponse<?>> filtered = tested.doFilter(request, chain);

		assertThat(reachedTheChain).containsExactly(request);
		assertThat(filtered).isSameAs(passedThrough);
	}

	private static MutableHttpRequest<?> requestWithCookie(String name, String value) {
		return new SimpleHttpRequestFactory().get("/overview").cookie(Cookie.of(name, value));
	}

	private static Optional<Login> loginOn(HttpRequest<?> request) {
		return request.getAttribute(LoginAttribute.NAME, Login.class);
	}

	private static final class SignsOnlyOneValue implements SignedLogins {
		@Override
		public String issue(String accountName, Role role, long generation) {
			throw new UnsupportedOperationException();
		}

		@Override
		public String issue(String accountName, Role role, long generation, Instant expiresAt) {
			throw new UnsupportedOperationException();
		}

		@Override
		public String issueForTheAdminKey(String name) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Optional<Login> read(String value) {
			return SIGNED_VALUE.equals(value) ? Optional.of(AURORA) : Optional.empty();
		}
	}
}
