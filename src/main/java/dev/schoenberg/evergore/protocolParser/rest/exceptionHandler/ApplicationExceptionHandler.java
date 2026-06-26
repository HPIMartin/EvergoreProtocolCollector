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
import dev.schoenberg.evergore.protocolParser.exceptions.TooManyRequests;

import static io.micronaut.http.HttpResponse.status;
import static io.micronaut.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static io.micronaut.http.HttpStatus.NOT_FOUND;
import static io.micronaut.http.HttpStatus.TOO_MANY_REQUESTS;
import static io.micronaut.http.HttpStatus.UNAUTHORIZED;

@Produces
@Singleton
@Requires(classes = {ProtocolParserException.class})
public class ApplicationExceptionHandler implements ExceptionHandler<ProtocolParserException, HttpResponse<?>> {
	private Logger logger;

	public ApplicationExceptionHandler(Logger logger) {
		this.logger = logger;
	}

	@Override
	public HttpResponse<?> handle(@SuppressWarnings("rawtypes") HttpRequest request, ProtocolParserException exception) {
		String reason = "Exception while requesting: " + request.getPath();

		if (exception instanceof AccessNotAllowed || exception instanceof TooManyRequests) {
			exception.setStackTrace(new StackTraceElement[0]);
		}
		logger.error(reason, exception);

		if (exception instanceof AccessNotAllowed) {
			return status(UNAUTHORIZED);
		} else if (exception instanceof NoElementFound) {
			return status(NOT_FOUND, ((NoElementFound) exception).requestedValue);
		} else if (exception instanceof TooManyRequests) {
			return status(TOO_MANY_REQUESTS);
		} else {
			return status(INTERNAL_SERVER_ERROR);
		}
	}
}
