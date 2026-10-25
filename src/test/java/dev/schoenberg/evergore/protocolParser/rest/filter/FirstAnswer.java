package dev.schoenberg.evergore.protocolParser.rest.filter;

import java.util.concurrent.atomic.AtomicReference;

import io.micronaut.http.MutableHttpResponse;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

final class FirstAnswer implements Subscriber<MutableHttpResponse<?>> {
	private final AtomicReference<MutableHttpResponse<?>> value = new AtomicReference<>();

	static MutableHttpResponse<?> of(Publisher<MutableHttpResponse<?>> answer) {
		FirstAnswer first = new FirstAnswer();
		answer.subscribe(first);
		return first.value.get();
	}

	@Override
	public void onSubscribe(Subscription subscription) {
		subscription.request(1);
	}

	@Override
	public void onNext(MutableHttpResponse<?> next) {
		value.set(next);
	}

	@Override
	public void onError(Throwable error) {
		throw new AssertionError(error);
	}

	@Override
	public void onComplete() {}
}
