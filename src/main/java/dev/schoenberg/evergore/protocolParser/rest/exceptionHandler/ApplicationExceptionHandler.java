package dev.schoenberg.evergore.protocolParser.rest.exceptionHandler;

import jakarta.inject.Singleton;

import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.server.exceptions.ExceptionHandler;

import dev.schoenberg.evergore.protocolParser.Logger;
import dev.schoenberg.evergore.protocolParser.exceptions.AccessNotAllowed;
import dev.schoenberg.evergore.protocolParser.exceptions.NoElementFound;
import dev.schoenberg.evergore.protocolParser.exceptions.ProtocolParserException;
import dev.schoenberg.evergore.protocolParser.exceptions.ProtocolParserException.ExceptionResponseVisitor;
import dev.schoenberg.evergore.protocolParser.exceptions.TooManyRequests;

import static io.micronaut.http.HttpResponse.status;
import static io.micronaut.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static io.micronaut.http.HttpStatus.NOT_FOUND;
import static io.micronaut.http.HttpStatus.TOO_MANY_REQUESTS;
import static io.micronaut.http.HttpStatus.UNAUTHORIZED;

@Produces
@Singleton
@Requires(classes = {ProtocolParserException.class})
public class ApplicationExceptionHandler implements ExceptionHandler<ProtocolParserException, HttpResponse<?>>, ExceptionResponseVisitor<HttpResponse<?>> {
	private final Logger logger;

	public ApplicationExceptionHandler(Logger logger) {
		this.logger = logger;
	}

	@Override
	public HttpResponse<?> handle(@SuppressWarnings("rawtypes") HttpRequest request, ProtocolParserException exception) {
		String reason = "Exception while requesting: " + request.getPath();
		logger.error(reason, exception);
		return exception.accept(this);
	}

	@Override
	public HttpResponse<?> onAccessNotAllowed(AccessNotAllowed exception) {
		exception.setStackTrace(new StackTraceElement[0]);
		return status(UNAUTHORIZED);
	}

	@Override
	public HttpResponse<?> onNoElementFound(NoElementFound exception) {
		return status(NOT_FOUND, exception.requestedValue);
	}

	@Override
	public HttpResponse<?> onTooManyRequests(TooManyRequests exception) {
		exception.setStackTrace(new StackTraceElement[0]);
		return status(TOO_MANY_REQUESTS);
	}

	@Override
	public HttpResponse<?> onUnknown(ProtocolParserException exception) {
		return status(INTERNAL_SERVER_ERROR);
	}
}
