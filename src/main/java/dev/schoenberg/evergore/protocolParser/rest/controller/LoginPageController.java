package dev.schoenberg.evergore.protocolParser.rest.controller;

import java.io.InputStream;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Produces;

import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.silentThrow;

@Controller
public class LoginPageController {
	public static final String PATH = "/login";

	private static final String LOGIN_PAGE_RESOURCE = "/static/ui/login/login.html";

	@Get(PATH)
	@Produces(MediaType.TEXT_HTML)
	public HttpResponse<byte[]> loginPage() {
		return HttpResponse.ok(loadLoginPage()).contentType(MediaType.TEXT_HTML_TYPE).header("Cache-Control", "no-cache");
	}

	private byte[] loadLoginPage() {
		return silentThrow(() -> {
			try (InputStream page = getClass().getResourceAsStream(LOGIN_PAGE_RESOURCE)) {
				return page.readAllBytes();
			}
		});
	}
}
