package dev.schoenberg.evergore.protocolParser.rest.filter;

import java.util.concurrent.atomic.AtomicReference;

import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.filter.ServerFilterChain;
import io.micronaut.http.netty.body.NettyByteBodyFactory;
import io.micronaut.http.server.HttpServerConfiguration;
import io.micronaut.http.server.netty.NettyHttpRequest;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import dev.schoenberg.evergore.protocolParser.rest.netty.UnreadableRequest;
import dev.schoenberg.evergore.protocolParser.rest.netty.UnreadableRequests;

import static org.assertj.core.api.Assertions.assertThat;

class UnreadableRequestFilterTest {
	private final EmbeddedChannel channel = new EmbeddedChannel(new ChannelInboundHandlerAdapter());
	private final UnreadableRequestFilter tested = new UnreadableRequestFilter();
	private final RecordingChain chain = new RecordingChain();

	@AfterEach
	void closeChannel() {
		channel.finishAndReleaseAll();
	}

	@Test
	void answersARequestWhoseTargetWasTooLongWith413WithoutLettingItProceed() {
		HttpRequest<?> request = unreadable(HttpResponseStatus.REQUEST_ENTITY_TOO_LARGE);

		int answered = statusOf(tested.doFilter(request, chain));

		assertThat(answered).isEqualTo(413);
		assertThat(chain.proceeded()).isNull();
	}

	@Test
	void answersARequestWhoseHeadWasMalformedWith400WithoutLettingItProceed() {
		HttpRequest<?> request = unreadable(HttpResponseStatus.BAD_REQUEST);

		int answered = statusOf(tested.doFilter(request, chain));

		assertThat(answered).isEqualTo(400);
		assertThat(chain.proceeded()).isNull();
	}

	@Test
	void answersTheHeaderlessStandInOfAnUnreadableTargetWith400WithoutLettingItProceed() {
		HttpRequest<?> request = served(new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/"));

		int answered = statusOf(tested.doFilter(request, chain));

		assertThat(answered).isEqualTo(400);
		assertThat(chain.proceeded()).isNull();
	}

	@Test
	void letsARequestThatCarriesAHeaderProceed() {
		DefaultHttpRequest nativeRequest = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/overview");
		nativeRequest.headers().set(HttpHeaderNames.HOST, "localhost");
		HttpRequest<?> request = served(nativeRequest);

		tested.doFilter(request, chain);

		assertThat(chain.proceeded()).isSameAs(request);
	}

	@Test
	void letsAnyOtherRequestProceed() {
		HttpRequest<?> request = HttpRequest.GET("/bad-request");

		tested.doFilter(request, chain);

		assertThat(chain.proceeded()).isSameAs(request);
	}

	private HttpRequest<?> served(io.netty.handler.codec.http.HttpRequest nativeRequest) {
		return new NettyHttpRequest<>(nativeRequest, NettyByteBodyFactory.empty(), channel.pipeline().firstContext(), ConversionService.SHARED, new HttpServerConfiguration());
	}

	private HttpRequest<?> unreadable(HttpResponseStatus status) {
		UnreadableRequest nativeRequest = UnreadableRequests.standingInFor(status, new DefaultHttpHeaders());

		return served(nativeRequest);
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
