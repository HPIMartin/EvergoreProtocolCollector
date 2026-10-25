package dev.schoenberg.evergore.protocolParser.rest.controller.api;

import java.util.Objects;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Consumes;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Produces;

import dev.schoenberg.evergore.protocolParser.application.ChangePassword;
import dev.schoenberg.evergore.protocolParser.application.ChangePasswordOutcome;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.Login;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.PasswordRefusal;
import dev.schoenberg.evergore.protocolParser.exceptions.AccessNotAllowed;
import dev.schoenberg.evergore.protocolParser.rest.controller.api.wire.ChangePasswordRequest;
import dev.schoenberg.evergore.protocolParser.rest.controller.api.wire.LoginRefusal;
import dev.schoenberg.evergore.protocolParser.rest.filter.LoginAttribute;

import static io.micronaut.http.MediaType.APPLICATION_JSON;

@Controller(PasswordController.PATH)
public class PasswordController {
	public static final String PATH = "/api/v1/password";

	private final ChangePassword changePassword;

	public PasswordController(ChangePassword changePassword) {
		this.changePassword = changePassword;
	}

	@Post
	@Consumes(APPLICATION_JSON)
	@Produces(APPLICATION_JSON)
	public HttpResponse<?> change(HttpRequest<?> request, @Body ChangePasswordRequest body) {
		Login login = request.getAttribute(LoginAttribute.NAME, Login.class).orElseThrow(AccessNotAllowed::new);
		ChangePasswordOutcome outcome = changePassword.with(login, textOf(body.currentPassword()), textOf(body.newPassword()));

		return outcome.accept(new Answer());
	}

	private static String textOf(String field) {
		return Objects.requireNonNullElse(field, "");
	}

	private static final class Answer implements ChangePasswordOutcome.Visitor<HttpResponse<?>> {
		@Override
		public HttpResponse<?> onChanged(String loginValue) {
			return HttpResponse.noContent().header(SessionCookie.HEADER, SessionCookie.holding(loginValue));
		}

		@Override
		public HttpResponse<?> onNoAccount() {
			return HttpResponse.status(HttpStatus.FORBIDDEN).body(LoginRefusal.NO_ACCOUNT);
		}

		@Override
		public HttpResponse<?> onLoginEnded() {
			return HttpResponse.status(HttpStatus.UNAUTHORIZED);
		}

		@Override
		public HttpResponse<?> onWrongCurrentPassword() {
			return HttpResponse.badRequest(LoginRefusal.WRONG_CURRENT_PASSWORD);
		}

		@Override
		public HttpResponse<?> onNewPasswordRefused(PasswordRefusal refusal) {
			return HttpResponse.badRequest(LoginRefusal.of(refusal));
		}
	}
}
