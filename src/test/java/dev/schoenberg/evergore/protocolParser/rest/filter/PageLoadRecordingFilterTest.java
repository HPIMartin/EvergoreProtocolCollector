package dev.schoenberg.evergore.protocolParser.rest.filter;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.http.HttpMethod;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.filter.ServerFilterChain;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import dev.schoenberg.evergore.protocolParser.application.RecordedUsage;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.Login;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.LoginSource;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.Role;
import dev.schoenberg.evergore.protocolParser.businessLogic.usage.UsageKind;
import dev.schoenberg.evergore.protocolParser.businessLogic.usage.UsageRecord;

import static org.assertj.core.api.Assertions.assertThat;

class PageLoadRecordingFilterTest {
	private static final Instant NOW = Instant.parse("2026-01-05T10:00:00Z");
	private static final Login AERGER = new Login("Ärger", Role.MEMBER, 0, LoginSource.ACCOUNT, "id", NOW, NOW.plus(Login.LIFETIME));

	private final RecordedUsage usage = new RecordedUsage();
	private final PageLoadRecordingFilter tested = new PageLoadRecordingFilter(new PathCanonicalizer(), new SpaNavigationPaths(), usage, Clock.fixed(NOW, ZoneOffset.UTC));

	@Test
	void recordsAPageLoadUnderTheFoldedNameOfTheLoginsAccountForANavigationAnsweredWithThePage() {
		MutableHttpRequest<?> request = HttpRequest.GET("/overview").accept(MediaType.TEXT_HTML_TYPE);
		request.setAttribute(LoginAttribute.NAME, AERGER);

		FirstAnswer.of(tested.doFilter(request, answering(HttpStatus.OK)));

		assertThat(usage.records()).containsExactly(new UsageRecord("ärger", NOW, UsageKind.PAGE_LOAD, Optional.empty()));
	}

	@Test
	void recordsNoPageLoadForANavigationWithoutALogin() {
		MutableHttpRequest<?> request = HttpRequest.GET("/overview").accept(MediaType.TEXT_HTML_TYPE);

		FirstAnswer.of(tested.doFilter(request, answering(HttpStatus.OK)));

		assertThat(usage.records()).isEmpty();
	}

	@ParameterizedTest
	@CsvSource({"GET, /api/v1/overview, text/html", "GET, /assets/index.js, text/html", "GET, /favicon.ico, text/html", "GET, /health, text/html", "GET, /login, text/html",
			"GET, /overview, application/json", "POST, /overview, text/html"})
	void recordsNoPageLoadForARequestThatIsNoNavigationToAPageOfTheDashboard(String method, String path, String accept) {
		MutableHttpRequest<?> request = HttpRequest.create(HttpMethod.valueOf(method), path).accept(MediaType.of(accept));
		request.setAttribute(LoginAttribute.NAME, AERGER);

		FirstAnswer.of(tested.doFilter(request, answering(HttpStatus.OK)));

		assertThat(usage.records()).isEmpty();
	}

	@ParameterizedTest
	@EnumSource(value = HttpStatus.class, names = {"FOUND", "NOT_FOUND", "TOO_MANY_REQUESTS", "INTERNAL_SERVER_ERROR"})
	void recordsNoPageLoadForANavigationNotAnsweredWithThePage(HttpStatus status) {
		MutableHttpRequest<?> request = HttpRequest.GET("/overview").accept(MediaType.TEXT_HTML_TYPE);
		request.setAttribute(LoginAttribute.NAME, AERGER);

		FirstAnswer.of(tested.doFilter(request, answering(status)));

		assertThat(usage.records()).isEmpty();
	}

	private static ServerFilterChain answering(HttpStatus status) {
		return request -> Publishers.just(HttpResponse.status(status));
	}
}
