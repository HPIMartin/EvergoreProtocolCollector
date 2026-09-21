package dev.schoenberg.evergore.protocolParser.rest.filter;

import java.util.ArrayList;
import java.util.List;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.filter.ServerFilterChain;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;

import dev.schoenberg.evergore.protocolParser.LoggerSpy;
import dev.schoenberg.evergore.protocolParser.exceptions.AccessNotAllowed;
import dev.schoenberg.evergore.protocolParser.helper.config.SecurityConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TokenValidationFilterTest {
	private static final String API_TOKEN = "correct-horse-battery-staple";
	private static final String PROTECTED_PATH = "/api/v1/avatars";

	private final Publisher<MutableHttpResponse<?>> passedThrough = _ -> {};
	private final List<HttpRequest<?>> reachedTheChain = new ArrayList<>();
	private final ServerFilterChain chain = request -> {
		reachedTheChain.add(request);
		return passedThrough;
	};

	@Test
	void passesARequestCarryingTheApiTokenOnToTheChain() {
		TokenValidationFilter tested = filterExpecting(API_TOKEN);
		HttpRequest<?> request = requestWithToken(API_TOKEN);

		Publisher<MutableHttpResponse<?>> filtered = tested.doFilter(request, chain);

		assertThat(reachedTheChain).containsExactly(request);
		assertThat(filtered).isSameAs(passedThrough);
	}

	@Test
	void rejectsAnUnrelatedToken() {
		assertRejects("something-else-entirely");
	}

	@Test
	void rejectsAnEmptyToken() {
		assertRejects("");
	}

	@Test
	void rejectsATokenThatIsOnlyAPrefixOfTheApiToken() {
		assertRejects(API_TOKEN.substring(0, API_TOKEN.length() - 1));
	}

	@Test
	void rejectsATokenThatStartsWithTheApiTokenButRunsOn() {
		assertRejects(API_TOKEN + "x");
	}

	@Test
	void rejectsATokenDifferingOnlyInItsLastCharacter() {
		assertRejects(API_TOKEN.substring(0, API_TOKEN.length() - 1) + "X");
	}

	@Test
	void rejectsARequestWithoutATokenParameter() {
		TokenValidationFilter tested = filterExpecting(API_TOKEN);
		HttpRequest<?> request = HttpRequest.GET(PROTECTED_PATH);

		assertThatThrownBy(() -> tested.doFilter(request, chain)).isInstanceOf(AccessNotAllowed.class);
	}

	@Test
	void rejectsEveryTokenWhenNoApiTokenIsConfigured() {
		TokenValidationFilter tested = filterExpecting(null);
		HttpRequest<?> request = requestWithToken(API_TOKEN);

		assertThatThrownBy(() -> tested.doFilter(request, chain)).as("a missing configuration must deny, not crash").isInstanceOf(AccessNotAllowed.class);
	}

	@Test
	void rejectsAnEmptyTokenWhenTheConfiguredApiTokenIsEmptyToo() {
		assertUnusableConfiguration("");
	}

	@Test
	void rejectsABlankTokenWhenTheConfiguredApiTokenIsBlankToo() {
		assertUnusableConfiguration("   ");
	}

	private void assertUnusableConfiguration(String configuredApiToken) {
		TokenValidationFilter tested = filterExpecting(configuredApiToken);
		HttpRequest<?> request = requestWithToken(configuredApiToken);

		assertThatThrownBy(() -> tested.doFilter(request, chain)).as("an unusable configuration must deny even the value it holds").isInstanceOf(AccessNotAllowed.class);
	}

	private void assertRejects(String presentedToken) {
		TokenValidationFilter tested = filterExpecting(API_TOKEN);
		HttpRequest<?> request = requestWithToken(presentedToken);

		assertThatThrownBy(() -> tested.doFilter(request, chain)).isInstanceOf(AccessNotAllowed.class);
	}

	private TokenValidationFilter filterExpecting(String apiToken) {
		SecurityConfiguration configuration = new SecurityConfiguration(apiToken, List.of());
		return new TokenValidationFilter(configuration, new PathCanonicalizer(), new PublicPaths(configuration), new LoggerSpy());
	}

	private HttpRequest<?> requestWithToken(String token) {
		MutableHttpRequest<?> request = HttpRequest.GET(PROTECTED_PATH);
		request.getParameters().add("token", token);
		return request;
	}
}
