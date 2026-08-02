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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApplicationExceptionHandlerTest {

	private static final HttpRequest<?> GET_REQUEST = HttpRequest.GET("/test");
	private static final String SECRET_TOKEN = "s3cr3t-api-token";

	private final LoggerSpy logger = new LoggerSpy();
	private final ApplicationExceptionHandler handler = new ApplicationExceptionHandler(logger);

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

	@Test
	void logsThePathWithoutTheTokenQueryParameter() {
		handler.handle(HttpRequest.GET("/overview?token=" + SECRET_TOKEN), new AccessNotAllowed());

		assertThat(logger.infoMessages()).containsExactly("Exception while requesting: /overview");
	}

	@Test
	void accessNotAllowedIsLoggedAsOneLineWithoutTheStackTrace() {
		handle(new AccessNotAllowed());

		assertLoggedWithoutStackTrace("Exception while requesting: /test");
	}

	@Test
	void tooManyRequestsIsLoggedAsOneLineWithoutTheStackTrace() {
		handle(new TooManyRequests("some-id"));

		assertLoggedWithoutStackTrace("Exception while requesting: /test");
	}

	@Test
	void noElementFoundIsLoggedAsOneLineWithoutTheStackTrace() {
		handle(new NoElementFound("some-value"));

		assertLoggedWithoutStackTrace("Exception while requesting: /test");
	}

	@Test
	void unknownExceptionIsLoggedWithItsStackTrace() {
		UnknownException exception = new UnknownException();

		handle(exception);

		assertLoggedWithStackTrace("Exception while requesting: /test", exception);
	}

	@Test
	void aFailingResponseMappingIsStillLoggedWithItsStackTrace() {
		FailingMappingException exception = new FailingMappingException();

		assertThatThrownBy(() -> handle(exception)).isInstanceOf(IllegalStateException.class);

		assertLoggedWithStackTrace("Exception while requesting: /test", exception);
	}

	private HttpResponse<?> handle(ProtocolParserException exception) {
		return handler.handle(GET_REQUEST, exception);
	}

	private void assertLoggedWithoutStackTrace(String expectedMessage) {
		assertThat(logger.infoMessages()).containsExactly(expectedMessage);
		assertThat(logger.errorMessages()).isEmpty();
	}

	private void assertLoggedWithStackTrace(String expectedMessage, Throwable expectedCause) {
		assertThat(logger.errorMessages()).containsExactly(expectedMessage);
		assertThat(logger.errorThrowables()).containsExactly(expectedCause);
		assertThat(logger.infoMessages()).isEmpty();
	}

	private static class FailingMappingException extends ProtocolParserException {
		@Override
		public <T> T accept(ExceptionResponseVisitor<T> visitor) {
			throw new IllegalStateException("mapping this exception to a response fails");
		}
	}

	private static class UnknownException extends ProtocolParserException {
		@Override
		public <T> T accept(ExceptionResponseVisitor<T> visitor) {
			return visitor.onUnknown(this);
		}
	}
}
