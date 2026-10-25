package dev.schoenberg.evergore.protocolParser.rest.filter;

import java.util.concurrent.atomic.AtomicReference;

import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.filter.ServerFilterChain;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import static org.assertj.core.api.Assertions.assertThat;

class LoginBodyLimitFilterTest {
	private static final int FOUR_KIBIBYTES = 4096;

	private final LoginBodyLimitFilter tested = new LoginBodyLimitFilter(new PathCanonicalizer());
	private final RecordingChain chain = new RecordingChain();

	@Test
	void letsABodyOfExactlyFourKibibytesProceed() {
		HttpRequest<?> request = HttpRequest.POST("/api/v1/login", "body").contentLength(FOUR_KIBIBYTES);

		tested.doFilter(request, chain);

		assertThat(chain.proceeded()).isSameAs(request);
	}

	@Test
	void answers413ForABodyOneByteOverFourKibibytesWithoutLettingItProceed() {
		HttpRequest<?> request = HttpRequest.POST("/api/v1/login", "body").contentLength(FOUR_KIBIBYTES + 1);

		int answered = statusOf(tested.doFilter(request, chain));

		assertThat(answered).isEqualTo(413);
		assertThat(chain.proceeded()).isNull();
	}

	@Test
	void answers413ForABodyWhoseLengthIsNotDeclared() {
		HttpRequest<?> request = HttpRequest.POST("/api/v1/login", "body").header("Transfer-Encoding", "chunked");

		int answered = statusOf(tested.doFilter(request, chain));

		assertThat(answered).isEqualTo(413);
		assertThat(chain.proceeded()).isNull();
	}

	@ParameterizedTest
	@ValueSource(strings = {"gzip, chunked", "identity, chunked", "CHUNKED", "chunked, identity"})
	void answers413ForAnyListOfTransferEncodingsThatNamesChunked(String transferEncodings) {
		HttpRequest<?> request = HttpRequest.POST("/api/v1/login", "body").contentLength(FOUR_KIBIBYTES).header("Transfer-Encoding", transferEncodings);

		int answered = statusOf(tested.doFilter(request, chain));

		assertThat(answered).isEqualTo(413);
		assertThat(chain.proceeded()).isNull();
	}

	@Test
	void answers413ForChunkedNamedInASecondTransferEncodingLine() {
		MutableHttpRequest<String> request = HttpRequest.POST("/api/v1/login", "body").contentLength(FOUR_KIBIBYTES);
		request.getHeaders().add("Transfer-Encoding", "identity");
		request.getHeaders().add("Transfer-Encoding", "chunked");

		int answered = statusOf(tested.doFilter(request, chain));

		assertThat(answered).isEqualTo(413);
		assertThat(chain.proceeded()).isNull();
	}

	@ParameterizedTest
	@ValueSource(strings = {"gzip", "identity", "deflate", "br"})
	void answers413ForAnyTransferEncodingEvenWithoutChunked(String encoding) {
		HttpRequest<?> request = HttpRequest.POST("/api/v1/login", "body").contentLength(FOUR_KIBIBYTES).header("Transfer-Encoding", encoding);

		int answered = statusOf(tested.doFilter(request, chain));

		assertThat(answered).isEqualTo(413);
	}

	@ParameterizedTest
	@ValueSource(strings = {"gzip", "identity", "deflate", "br"})
	void answers413ForAnyContentEncodingBecauseTheLimitIsTheDecodedSize(String encoding) {
		HttpRequest<?> request = HttpRequest.POST("/api/v1/login", "body").contentLength(20).header("Content-Encoding", encoding);

		int answered = statusOf(tested.doFilter(request, chain));

		assertThat(answered).isEqualTo(413);
		assertThat(chain.proceeded()).isNull();
	}

	@ParameterizedTest
	@ValueSource(strings = {"/api/v1/login", "/api/v1/login/new-password", "/api/v1/password"})
	void answers413ForABodyWithNoDeclaredLengthOnEveryLimitedPath(String path) {
		HttpRequest<?> request = HttpRequest.POST(path, "body");

		int answered = statusOf(tested.doFilter(request, chain));

		assertThat(answered).isEqualTo(413);
		assertThat(chain.proceeded()).isNull();
	}

	@Test
	void letsABodyOfZeroBytesWithADeclaredLengthProceed() {
		HttpRequest<?> request = HttpRequest.POST("/api/v1/login", "").contentLength(0);

		tested.doFilter(request, chain);

		assertThat(chain.proceeded()).isSameAs(request);
	}

	@ParameterizedTest
	@ValueSource(strings = {"/api/v1/login/new-password", "/api//v1/login", "/api/v1/./login", "/api/v1/login/"})
	void limitsTheBodiesOfThePathsBelowTheLoginAndOfItsSpellingsToo(String path) {
		HttpRequest<?> request = HttpRequest.POST(path, "body").contentLength(FOUR_KIBIBYTES + 1);

		int answered = statusOf(tested.doFilter(request, chain));

		assertThat(answered).isEqualTo(413);
	}

	@ParameterizedTest
	@ValueSource(strings = {"/api/v1/password", "/api//v1/password", "/api/v1/./password"})
	void limitsTheBodyOfTheChangeOfAPasswordAndOfItsSpellingsToo(String path) {
		HttpRequest<?> request = HttpRequest.POST(path, "body").contentLength(FOUR_KIBIBYTES + 1);

		int answered = statusOf(tested.doFilter(request, chain));

		assertThat(answered).isEqualTo(413);
	}

	@Test
	void leavesTheBodiesOfEveryOtherPathAlone() {
		HttpRequest<?> request = HttpRequest.POST("/api/v1/avatars", "body").contentLength(FOUR_KIBIBYTES + 1);

		tested.doFilter(request, chain);

		assertThat(chain.proceeded()).isSameAs(request);
	}

	@Test
	void leavesAPathThatOnlyStartsLikeTheLoginAlone() {
		HttpRequest<?> request = HttpRequest.POST("/api/v1/loginfo", "body").contentLength(FOUR_KIBIBYTES + 1);

		tested.doFilter(request, chain);

		assertThat(chain.proceeded()).isSameAs(request);
	}

	@Test
	void letsARequestWithoutABodyProceed() {
		HttpRequest<?> request = HttpRequest.GET("/api/v1/login");

		tested.doFilter(request, chain);

		assertThat(chain.proceeded()).isSameAs(request);
	}

	private static int statusOf(Publisher<MutableHttpResponse<?>> answer) {
		FirstElement<MutableHttpResponse<?>> first = new FirstElement<>();
		answer.subscribe(first);
		return first.value().status().getCode();
	}

	private static final class FirstElement<T> implements Subscriber<T> {
		private final AtomicReference<T> value = new AtomicReference<>();

		@Override
		public void onSubscribe(Subscription subscription) {
			subscription.request(1);
		}

		@Override
		public void onNext(T next) {
			value.set(next);
		}

		@Override
		public void onError(Throwable error) {
			throw new AssertionError(error);
		}

		@Override
		public void onComplete() {}

		T value() {
			return value.get();
		}
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
