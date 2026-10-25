package dev.schoenberg.evergore.protocolParser.rest.filter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.filter.ServerFilterChain;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.reactivestreams.Publisher;

import dev.schoenberg.evergore.protocolParser.LoggerSpy;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.Login;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.LoginSource;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.Role;
import dev.schoenberg.evergore.protocolParser.exceptions.AccessNotAllowed;
import dev.schoenberg.evergore.protocolParser.exceptions.RoleMissing;
import dev.schoenberg.evergore.protocolParser.helper.config.SecurityConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccessFilterTest {
	private static final Instant MADE = Instant.parse("2026-01-05T10:00:00Z");
	private static final String OLD_TOKEN = "correct-horse-battery-staple";
	private static final String PROTECTED_PATH = "/api/v1/avatars";
	private static final Login AURORA = new Login("Aurora", Role.MEMBER, 0, LoginSource.ACCOUNT, "id", MADE, MADE.plus(Login.LIFETIME));

	private static final Login CALIX = new Login("Calix", Role.ADMIN, 0, LoginSource.ACCOUNT, "admin-id", MADE, MADE.plus(Login.LIFETIME));

	private final Publisher<MutableHttpResponse<?>> passedThrough = _ -> {};
	private final List<HttpRequest<?>> reachedTheChain = new ArrayList<>();
	private final ServerFilterChain chain = request -> {
		reachedTheChain.add(request);
		return passedThrough;
	};
	private final LoggerSpy logger = new LoggerSpy();
	private final SecurityConfiguration configuration = new SecurityConfiguration(List.of("/login", "/health", "/assets/**"));
	private final AccessFilter tested = new AccessFilter(new PathCanonicalizer(), new PublicPaths(configuration), new SpaNavigationPaths(), logger);

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

	@ParameterizedTest
	@ValueSource(strings = {"/api/v1/admin/status", "/api/v1/admin", "/api/v1/admin/", "/assets/../api/v1/admin/status"})
	void rejectsAMembersLoginOnTheAdminsPaths(String address) {
		MutableHttpRequest<?> request = HttpRequest.GET(address);
		request.setAttribute(LoginAttribute.NAME, AURORA);

		assertThatThrownBy(() -> tested.doFilter(request, chain)).isInstanceOf(RoleMissing.class);
		assertThat(reachedTheChain).isEmpty();
	}

	@Test
	void passesAnAdminLoginOnTheAdminsPaths() {
		MutableHttpRequest<?> request = HttpRequest.GET("/api/v1/admin/status");
		request.setAttribute(LoginAttribute.NAME, CALIX);

		Publisher<MutableHttpResponse<?>> filtered = tested.doFilter(request, chain);

		assertThat(reachedTheChain).containsExactly(request);
		assertThat(filtered).isSameAs(passedThrough);
	}

	@Test
	void passesAMembersLoginOnAPathThatOnlyStartsLikeTheAdminsPaths() {
		MutableHttpRequest<?> request = HttpRequest.GET("/api/v1/administrators");
		request.setAttribute(LoginAttribute.NAME, AURORA);

		tested.doFilter(request, chain);

		assertThat(reachedTheChain).containsExactly(request);
	}

	@Test
	void rejectsARequestWithoutALogin() {
		HttpRequest<?> request = HttpRequest.GET(PROTECTED_PATH);

		assertThatThrownBy(() -> tested.doFilter(request, chain)).isInstanceOf(AccessNotAllowed.class);
		assertThat(reachedTheChain).isEmpty();
	}

	@Test
	void rejectsARequestCarryingTheGuildsOldTokenAndNoLogin() {
		MutableHttpRequest<?> request = HttpRequest.GET(PROTECTED_PATH);
		request.getParameters().add("token", OLD_TOKEN);

		assertThatThrownBy(() -> tested.doFilter(request, chain)).isInstanceOf(AccessNotAllowed.class);
		assertThat(reachedTheChain).isEmpty();
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

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = {"/|/login?next=%2F", "/overview|/login?next=%2Foverview", "/this-page-does-not-exist|/login?next=%2Fthis-page-does-not-exist",
			"/avatars/Aurora/bank?page=2&sort=amount|/login?next=%2Favatars%2FAurora%2Fbank%3Fpage%3D2%26sort%3Damount", "/assets/../overview|/login?next=%2Foverview",
			"/overview?token=old|/login?next=%2Foverview%3Ftoken%3Dold"})
	void sendsANavigationWithoutALoginToTheLoginPageWithThePageAsTheWayBack(String address, String expectedLocation) {
		MutableHttpRequest<?> request = HttpRequest.GET(address).accept(MediaType.TEXT_HTML_TYPE);

		MutableHttpResponse<?> answer = FirstAnswer.of(tested.doFilter(request, chain));

		assertThat(answer.status().getCode()).isEqualTo(HttpStatus.FOUND.getCode());
		assertThat(answer.getHeaders().get("Location")).isEqualTo(expectedLocation);
		assertThat(reachedTheChain).isEmpty();
	}

	@ParameterizedTest
	@ValueSource(strings = {"/api/v1/avatars", "/favicon.ico", "/swagger-ui/index.html"})
	void rejectsAnAddressThatIsNoPageOfTheDashboardEvenWhenAskedForAsHtml(String address) {
		HttpRequest<?> request = HttpRequest.GET(address).accept(MediaType.TEXT_HTML_TYPE);

		assertThatThrownBy(() -> tested.doFilter(request, chain)).isInstanceOf(AccessNotAllowed.class);
	}

	@Test
	void rejectsAPageOfTheDashboardAskedForWithoutAskingForHtml() {
		HttpRequest<?> request = HttpRequest.GET("/overview").accept(MediaType.APPLICATION_JSON_TYPE);

		assertThatThrownBy(() -> tested.doFilter(request, chain)).isInstanceOf(AccessNotAllowed.class);
	}

	@Test
	void rejectsAPageOfTheDashboardAskedForWithoutAnyAcceptHeader() {
		HttpRequest<?> request = HttpRequest.GET("/overview");

		assertThatThrownBy(() -> tested.doFilter(request, chain)).isInstanceOf(AccessNotAllowed.class);
	}

	@Test
	void rejectsAPostToAPageOfTheDashboardEvenWhenItAsksForHtml() {
		HttpRequest<?> request = HttpRequest.POST("/overview", "").accept(MediaType.TEXT_HTML_TYPE);

		assertThatThrownBy(() -> tested.doFilter(request, chain)).isInstanceOf(AccessNotAllowed.class);
	}

	@Test
	void logsThatAccessWasRejectedForANavigationToo() {
		MutableHttpRequest<?> request = HttpRequest.GET("/overview").accept(MediaType.TEXT_HTML_TYPE);

		FirstAnswer.of(tested.doFilter(request, chain));

		assertThat(logger.infoMessages()).contains("Access rejected");
	}
}
