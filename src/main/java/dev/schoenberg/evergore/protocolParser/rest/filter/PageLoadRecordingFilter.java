package dev.schoenberg.evergore.protocolParser.rest.filter;

import java.time.Clock;
import java.util.Optional;

import jakarta.inject.Singleton;

import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.HttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import org.reactivestreams.Publisher;

import dev.schoenberg.evergore.protocolParser.application.UsageRecording;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.Login;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.LoginName;
import dev.schoenberg.evergore.protocolParser.businessLogic.usage.UsageKind;
import dev.schoenberg.evergore.protocolParser.businessLogic.usage.UsageRecord;

@Singleton
@Filter("/**")
public class PageLoadRecordingFilter implements HttpServerFilter {
	private final SpaNavigations navigations;
	private final UsageRecording usage;
	private final Clock clock;

	public PageLoadRecordingFilter(SpaNavigations navigations, UsageRecording usage, Clock clock) {
		this.navigations = navigations;
		this.usage = usage;
		this.clock = clock;
	}

	@Override
	public int getOrder() {
		return FilterOrder.PAGE_LOAD_RECORDING.position();
	}

	@Override
	public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {
		return Publishers.map(chain.proceed(request), response -> {
			if (navigations.isOne(request) && response.getStatus() == HttpStatus.OK) {
				request.getAttribute(LoginAttribute.NAME, Login.class).ifPresent(this::recordAPageLoadOf);
			}
			return response;
		});
	}

	private void recordAPageLoadOf(Login login) {
		usage.record(new UsageRecord(new LoginName(login.accountName()).key(), clock.instant(), UsageKind.PAGE_LOAD, Optional.empty()));
	}
}
