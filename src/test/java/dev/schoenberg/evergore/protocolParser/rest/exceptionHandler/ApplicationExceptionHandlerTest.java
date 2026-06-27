package dev.schoenberg.evergore.protocolParser.rest.exceptionHandler;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import org.junit.jupiter.api.Test;

import dev.schoenberg.evergore.protocolParser.LoggerSpy;
import dev.schoenberg.evergore.protocolParser.exceptions.AccessNotAllowed;
import dev.schoenberg.evergore.protocolParser.exceptions.NoElementFound;
import dev.schoenberg.evergore.protocolParser.exceptions.ProtocolParserException;
import dev.schoenberg.evergore.protocolParser.exceptions.ProtocolParserException.ExceptionResponseVisitor;
import dev.schoenberg.evergore.protocolParser.exceptions.TooManyRequests;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationExceptionHandlerTest {

	private static final HttpRequest<?> GET_REQUEST = HttpRequest.GET("/test");

	private final ApplicationExceptionHandler handler = new ApplicationExceptionHandler(new LoggerSpy());

	@Test
	void accessNotAllowedMapsToUnauthorized() {
		HttpResponse<?> response = handle(new AccessNotAllowed());

		assertThat(response.getStatus().getCode()).isEqualTo(HttpStatus.UNAUTHORIZED.getCode());
	}

	@Test
	void noElementFoundMapsToNotFound() {
		HttpResponse<?> response = handle(new NoElementFound("some-value"));

		assertThat(response.getStatus().getCode()).isEqualTo(HttpStatus.NOT_FOUND.getCode());
	}

	@Test
	void tooManyRequestsMapsToTooManyRequests() {
		HttpResponse<?> response = handle(new TooManyRequests("some-id"));

		assertThat(response.getStatus().getCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.getCode());
	}

	@Test
	void unknownExceptionMapsToInternalServerError() {
		HttpResponse<?> response = handle(new UnknownException());

		assertThat(response.getStatus().getCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.getCode());
	}

	private HttpResponse<?> handle(ProtocolParserException exception) {
		return handler.handle(GET_REQUEST, exception);
	}

	private static class UnknownException extends ProtocolParserException {
		@Override
		public <T> T accept(ExceptionResponseVisitor<T> visitor) {
			return visitor.onUnknown(this);
		}
	}
}
