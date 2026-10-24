package dev.schoenberg.evergore.protocolParser.rest.exceptionHandler;

import jakarta.inject.Singleton;

import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.server.exceptions.ExceptionHandler;
import io.netty.handler.codec.compression.DecompressionException;

import dev.schoenberg.evergore.protocolParser.Logger;

import static io.micronaut.http.HttpResponse.status;
import static io.micronaut.http.HttpStatus.BAD_REQUEST;

@Produces
@Singleton
@Requires(classes = {DecompressionException.class})
public class UndecodableBodyHandler implements ExceptionHandler<DecompressionException, HttpResponse<?>> {
	private final Logger logger;

	public UndecodableBodyHandler(Logger logger) {
		this.logger = logger;
	}

	@Override
	public HttpResponse<?> handle(@SuppressWarnings("rawtypes") HttpRequest request, DecompressionException exception) {
		logger.warn("Request to " + request.getPath() + " carried a body its declared encoding does not decode");
		return status(BAD_REQUEST);
	}
}
