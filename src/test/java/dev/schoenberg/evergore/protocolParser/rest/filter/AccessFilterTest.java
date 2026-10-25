package dev.schoenberg.evergore.protocolParser.rest.filter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.filter.ServerFilterChain;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;

import dev.schoenberg.evergore.protocolParser.LoggerSpy;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.Login;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.LoginSource;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.Role;
import dev.schoenberg.evergore.protocolParser.exceptions.AccessNotAllowed;
import dev.schoenberg.evergore.protocolParser.helper.config.SecurityConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccessFilterTest {
	private static final Instant MADE = Instant.parse("2026-01-05T10:00:00Z");
	private static final String GUILD_TOKEN = "correct-horse-battery-staple";
	private static final String PROTECTED_PATH = "/api/v1/avatars";
	private static final Login AURORA = new Login("Aurora", Role.MEMBER, 0, LoginSource.ACCOUNT, "id", MADE, MADE.plus(Login.LIFETIME));

	private final Publisher<MutableHttpResponse<?>> passedThrough = _ -> {};
	private final List<HttpRequest<?>> reachedTheChain = new ArrayList<>();
	private final ServerFilterChain chain = request -> {
		reachedTheChain.add(request);
		return passedThrough;
	};
	private final LoggerSpy logger = new LoggerSpy();
	private final SecurityConfiguration configuration = new SecurityConfiguration(Optional.of(GUILD_TOKEN), List.of("/login", "/health", "/assets/**"));
	private final AccessFilter tested = new AccessFilter(new PathCanonicalizer(), new PublicPaths(configuration), new GuildToken(configuration), logger);

	@Test
	void passesARequestForAPublicPathWithoutALogin() {
		HttpRequest<?> request = HttpRequest.GET("/login");

		Publisher<MutableHttpResponse<?>> filtered = tested.doFilter(request, chain);

		assertThat(reachedTheChain).containsExactly(request);
		assertThat(filtered).isSameAs(passedThrough);
	}

	@Test
	void passesARequestWhoseLoginTheLoginReadingFilterPutOnIt() {
		MutableHttpRequest<?> request = HttpRequest.GET(PROTECTED_PATH);
		request.setAttribute(LoginAttribute.NAME, AURORA);

		Publisher<MutableHttpResponse<?>> filtered = tested.doFilter(request, chain);

		assertThat(reachedTheChain).containsExactly(request);
		assertThat(filtered).isSameAs(passedThrough);
	}

	@Test
	void passesARequestCarryingTheGuildTokenWhileTheTokenStillOpens() {
		MutableHttpRequest<?> request = HttpRequest.GET(PROTECTED_PATH);
		request.getParameters().add("token", GUILD_TOKEN);

		Publisher<MutableHttpResponse<?>> filtered = tested.doFilter(request, chain);

		assertThat(reachedTheChain).containsExactly(request);
		assertThat(filtered).isSameAs(passedThrough);
	}

	@Test
	void rejectsARequestWithNeitherALoginNorTheGuildToken() {
		HttpRequest<?> request = HttpRequest.GET(PROTECTED_PATH);

		assertThatThrownBy(() -> tested.doFilter(request, chain)).isInstanceOf(AccessNotAllowed.class);
		assertThat(reachedTheChain).isEmpty();
	}

	@Test
	void rejectsARequestCarryingAWrongGuildTokenAndNoLogin() {
		MutableHttpRequest<?> request = HttpRequest.GET(PROTECTED_PATH);
		request.getParameters().add("token", "wrong");

		assertThatThrownBy(() -> tested.doFilter(request, chain)).isInstanceOf(AccessNotAllowed.class);
	}

	@Test
	void rejectsAPathThatClimbsOutOfAPublicOneOntoAProtectedOne() {
		HttpRequest<?> request = HttpRequest.GET("/assets/../api/v1/avatars");

		assertThatThrownBy(() -> tested.doFilter(request, chain)).isInstanceOf(AccessNotAllowed.class);
	}

	@Test
	void logsThatAccessWasRejected() {
		HttpRequest<?> request = HttpRequest.GET(PROTECTED_PATH);

		assertThatThrownBy(() -> tested.doFilter(request, chain)).isInstanceOf(AccessNotAllowed.class);
		assertThat(logger.infoMessages()).contains("Access rejected");
	}
}
