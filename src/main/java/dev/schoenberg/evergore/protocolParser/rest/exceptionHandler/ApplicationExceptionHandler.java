package dev.schoenberg.evergore.protocolParser.rest.exceptionHandler;

import static io.micronaut.http.HttpResponse.*;
import static io.micronaut.http.HttpStatus.*;

import dev.schoenberg.evergore.protocolParser.*;
import dev.schoenberg.evergore.protocolParser.exceptions.*;
import io.micronaut.context.annotation.*;
import io.micronaut.http.*;
import io.micronaut.http.annotation.*;
import io.micronaut.http.server.exceptions.*;
import jakarta.inject.*;

@Produces
@Singleton
@Requires(classes = { ProtocolParserException.class })
public class ApplicationExceptionHandler implements ExceptionHandler<ProtocolParserException, HttpResponse<?>> {
	private Logger logger;

	public ApplicationExceptionHandler(Logger logger) {
		this.logger = logger;
	}

	@Override
	public HttpResponse<?> handle(@SuppressWarnings("rawtypes") HttpRequest request,
			ProtocolParserException exception) {
		logger.error("Exception while requesting: " + request.getPath(), exception);

		if (exception instanceof AccessNotAllowed) {
			return status(UNAUTHORIZED);
		} else if (exception instanceof NoElementFound) {
			return status(NOT_FOUND, ((NoElementFound) exception).requestedValue);
		} else {
			return status(INTERNAL_SERVER_ERROR);
		}
	}
}
