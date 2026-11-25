package dev.schoenberg.evergore.protocolParser.rest.controller;

import java.io.InputStream;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Error;

import dev.schoenberg.evergore.protocolParser.rest.filter.SpaNavigations;

import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.silentThrow;

@Controller
public class SpaHistoryFallbackController {
	private static final String INDEX_HTML_RESOURCE = "/static/ui/index.html";

	private final SpaNavigations navigations;

	public SpaHistoryFallbackController(SpaNavigations navigations) {
		this.navigations = navigations;
	}

	@Error(status = HttpStatus.NOT_FOUND, global = true)
	public HttpResponse<?> handleUnknownPath(HttpRequest<?> request) {
		if (!navigations.isOne(request)) {
			return HttpResponse.notFound();
		}

		return HttpResponse.ok(loadSpaShell()).contentType(MediaType.TEXT_HTML_TYPE).header("Cache-Control", "no-cache");
	}

	private byte[] loadSpaShell() {
		return silentThrow(() -> {
			try (InputStream shell = getClass().getResourceAsStream(INDEX_HTML_RESOURCE)) {
				return shell.readAllBytes();
			}
		});
	}
}
