package dev.schoenberg.evergore.protocolParser.rest.filter;

import java.util.List;
import java.util.Optional;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpRequest;
import org.junit.jupiter.api.Test;

import dev.schoenberg.evergore.protocolParser.helper.config.SecurityConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

class GuildTokenTest {
	private static final String API_TOKEN = "correct-horse-battery-staple";
	private static final String PROTECTED_PATH = "/api/v1/avatars";

	@Test
	void opensForARequestCarryingTheApiToken() {
		GuildToken tested = tokenExpecting(API_TOKEN);

		boolean opens = tested.opens(requestWithToken(API_TOKEN));

		assertThat(opens).isTrue();
	}

	@Test
	void doesNotOpenForAnUnrelatedToken() {
		assertDoesNotOpenFor("something-else-entirely");
	}

	@Test
	void doesNotOpenForAnEmptyToken() {
		assertDoesNotOpenFor("");
	}

	@Test
	void doesNotOpenForATokenThatIsOnlyAPrefixOfTheApiToken() {
		assertDoesNotOpenFor(API_TOKEN.substring(0, API_TOKEN.length() - 1));
	}

	@Test
	void doesNotOpenForATokenThatStartsWithTheApiTokenButRunsOn() {
		assertDoesNotOpenFor(API_TOKEN + "x");
	}

	@Test
	void doesNotOpenForATokenDifferingOnlyInItsLastCharacter() {
		assertDoesNotOpenFor(API_TOKEN.substring(0, API_TOKEN.length() - 1) + "X");
	}

	@Test
	void doesNotOpenForARequestWithoutATokenParameter() {
		GuildToken tested = tokenExpecting(API_TOKEN);

		boolean opens = tested.opens(HttpRequest.GET(PROTECTED_PATH));

		assertThat(opens).isFalse();
	}

	@Test
	void doesNotOpenForAnyTokenWhenNoApiTokenIsConfigured() {
		GuildToken tested = tokenConfiguredWith(Optional.empty());

		boolean opens = tested.opens(requestWithToken(API_TOKEN));

		assertThat(opens).as("a missing configuration must deny, not crash").isFalse();
	}

	@Test
	void doesNotOpenForAnEmptyTokenWhenTheConfiguredApiTokenIsEmptyToo() {
		assertUnusableConfiguration("");
	}

	@Test
	void doesNotOpenForABlankTokenWhenTheConfiguredApiTokenIsBlankToo() {
		assertUnusableConfiguration("   ");
	}

	private void assertUnusableConfiguration(String configuredApiToken) {
		GuildToken tested = tokenConfiguredWith(Optional.of(configuredApiToken));

		boolean opens = tested.opens(requestWithToken(configuredApiToken));

		assertThat(opens).as("an unusable configuration must deny even the value it holds").isFalse();
	}

	private void assertDoesNotOpenFor(String presentedToken) {
		GuildToken tested = tokenExpecting(API_TOKEN);

		boolean opens = tested.opens(requestWithToken(presentedToken));

		assertThat(opens).isFalse();
	}

	private GuildToken tokenExpecting(String apiToken) {
		return tokenConfiguredWith(Optional.of(apiToken));
	}

	private GuildToken tokenConfiguredWith(Optional<String> apiToken) {
		return new GuildToken(new SecurityConfiguration(apiToken, List.of()));
	}

	private HttpRequest<?> requestWithToken(String token) {
		MutableHttpRequest<?> request = HttpRequest.GET(PROTECTED_PATH);
		request.getParameters().add("token", token);
		return request;
	}
}
