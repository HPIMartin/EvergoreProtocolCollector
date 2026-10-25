package dev.schoenberg.evergore.protocolParser.rest.controller.api;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Produces;

import dev.schoenberg.evergore.protocolParser.application.LogOut;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.Login;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.LoginSource;
import dev.schoenberg.evergore.protocolParser.exceptions.AccessNotAllowed;
import dev.schoenberg.evergore.protocolParser.rest.controller.api.wire.Me;
import dev.schoenberg.evergore.protocolParser.rest.filter.LoginAttribute;

import static io.micronaut.http.MediaType.APPLICATION_JSON;

@Controller(SessionController.PATH)
public class SessionController {
	public static final String PATH = "/api/v1";

	private final LogOut logOut;

	public SessionController(LogOut logOut) {
		this.logOut = logOut;
	}

	@Get("/me")
	@Produces(APPLICATION_JSON)
	public Me me(HttpRequest<?> request) {
		Login login = loginOf(request);

		return new Me(login.accountName(), login.source() == LoginSource.ACCOUNT);
	}

	@Post("/logout")
	public HttpResponse<?> logOut(HttpRequest<?> request) {
		logOut.of(loginOf(request));

		return HttpResponse.noContent().header(SessionCookie.HEADER, SessionCookie.cleared());
	}

	private static Login loginOf(HttpRequest<?> request) {
		return request.getAttribute(LoginAttribute.NAME, Login.class).orElseThrow(AccessNotAllowed::new);
	}
}
