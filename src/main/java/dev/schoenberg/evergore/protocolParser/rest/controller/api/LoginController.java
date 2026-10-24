package dev.schoenberg.evergore.protocolParser.rest.controller.api;

import java.util.Objects;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Consumes;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Produces;

import dev.schoenberg.evergore.protocolParser.application.LogIn;
import dev.schoenberg.evergore.protocolParser.application.LogInOutcome;
import dev.schoenberg.evergore.protocolParser.rest.controller.api.wire.LoginRefusal;
import dev.schoenberg.evergore.protocolParser.rest.controller.api.wire.LoginRequest;
import dev.schoenberg.evergore.protocolParser.rest.controller.api.wire.LoginStep;

import static io.micronaut.http.MediaType.APPLICATION_JSON;

@Controller(LoginController.PATH)
public class LoginController {
	public static final String PATH = "/api/v1/login";

	private final LogIn logIn;

	public LoginController(LogIn logIn) {
		this.logIn = logIn;
	}

	@Post
	@Consumes(APPLICATION_JSON)
	@Produces(APPLICATION_JSON)
	public HttpResponse<?> logIn(@Body LoginRequest request) {
		LogInOutcome outcome = logIn.with(textOf(request.name()), textOf(request.password()));

		return outcome.accept(new Answer());
	}

	private static String textOf(String field) {
		return Objects.requireNonNullElse(field, "");
	}

	private static final class Answer implements LogInOutcome.Visitor<HttpResponse<?>> {
		@Override
		public HttpResponse<?> onLoggedIn(String loginValue) {
			return HttpResponse.ok(LoginStep.LOGGED_IN).header(SessionCookie.HEADER, SessionCookie.holding(loginValue));
		}

		@Override
		public HttpResponse<?> onChooseNewPassword() {
			return HttpResponse.ok(LoginStep.CHOOSE_NEW_PASSWORD);
		}

		@Override
		public HttpResponse<?> onRefused() {
			return HttpResponse.status(HttpStatus.UNAUTHORIZED).body(LoginRefusal.WRONG_NAME_OR_PASSWORD);
		}
	}
}
