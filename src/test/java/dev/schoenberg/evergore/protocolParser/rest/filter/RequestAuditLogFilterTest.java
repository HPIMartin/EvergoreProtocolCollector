package dev.schoenberg.evergore.protocolParser.rest.filter;

import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.filter.ServerFilterChain;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;

import dev.schoenberg.evergore.protocolParser.LoggerSpy;

import static org.assertj.core.api.Assertions.assertThat;

class RequestAuditLogFilterTest {
	private static final String CLIENT_IP = "10.0.0.7";
	private static final String USER_AGENT = "Firefox/141.0";

	private final LoggerSpy logger = new LoggerSpy();
	private final RequestAuditLogFilter tested = new RequestAuditLogFilter(new ClientIp(), logger);
	private final RecordingChain chain = new RecordingChain();

	@Test
	void logsOnlyTheClientIpAndTheUserAgentOfARequest() {
		HttpRequest<?> request = requestTo("/api/v1/avatars");

		tested.doFilter(request, chain);

		assertThat(logger.infoMessages()).containsExactly("Client IP: " + CLIENT_IP + " Agent: " + USER_AGENT);
	}

	@Test
	void logsARequestForTheSpaShellLikeAnyOther() {
		HttpRequest<?> request = requestTo("/");

		tested.doFilter(request, chain);

		assertThat(logger.infoMessages()).containsExactly("Client IP: " + CLIENT_IP + " Agent: " + USER_AGENT);
	}

	@Test
	void logsARequestForABundledAssetLikeAnyOther() {
		HttpRequest<?> request = requestTo("/assets/index-abc123.js");

		tested.doFilter(request, chain);

		assertThat(logger.infoMessages()).containsExactly("Client IP: " + CLIENT_IP + " Agent: " + USER_AGENT);
	}

	@Test
	void neverLogsTheQueryStringThatCarriesTheToken() {
		HttpRequest<?> request = requestTo("/api/v1/avatars?token=super-secret");

		tested.doFilter(request, chain);

		assertThat(logger.infoMessages()).noneMatch(message -> message.contains("super-secret"));
	}

	@Test
	void letsTheRequestThroughAfterLogging() {
		HttpRequest<?> request = requestTo("/");

		tested.doFilter(request, chain);

		assertThat(chain.proceeded()).isSameAs(request);
	}

	private HttpRequest<?> requestTo(String path) {
		return HttpRequest.GET("http://" + CLIENT_IP + ":8080" + path).header("user-agent", USER_AGENT);
	}

	private static final class RecordingChain implements ServerFilterChain {
		private HttpRequest<?> proceeded;

		@Override
		public Publisher<MutableHttpResponse<?>> proceed(HttpRequest<?> request) {
			proceeded = request;
			return Publishers.just(HttpResponse.ok());
		}

		HttpRequest<?> proceeded() {
			return proceeded;
		}
	}
}
