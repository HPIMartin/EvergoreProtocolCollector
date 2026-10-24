package dev.schoenberg.evergore.protocolParser.rest.exceptionHandler;

import jakarta.inject.Singleton;

import io.micronaut.context.annotation.Replaces;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.server.exceptions.response.ErrorContext;
import io.micronaut.http.server.exceptions.response.ErrorResponseProcessor;

import dev.schoenberg.evergore.protocolParser.Logger;

@Singleton
@Replaces(ErrorResponseProcessor.class)
public class BareErrorResponseProcessor implements ErrorResponseProcessor<Object> {
	private final Logger logger;

	public BareErrorResponseProcessor(Logger logger) {
		this.logger = logger;
	}

	@Override
	public MutableHttpResponse<Object> processResponse(ErrorContext errorContext, MutableHttpResponse<?> baseResponse) {
		HttpStatus status = baseResponse.status();
		String reason = "Request to " + errorContext.getRequest().getPath() + " answered " + status.getCode();
		if (status.getCode() >= HttpStatus.INTERNAL_SERVER_ERROR.getCode()) {
			errorContext.getRootCause().ifPresentOrElse(cause -> logger.error(reason, cause), () -> logger.error(reason));
		} else if (status == HttpStatus.BAD_REQUEST) {
			logger.warn(reason);
		}
		return HttpResponse.status(status);
	}
}
