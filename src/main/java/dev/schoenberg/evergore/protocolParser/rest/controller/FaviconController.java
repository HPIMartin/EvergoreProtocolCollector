package dev.schoenberg.evergore.protocolParser.rest.controller;

import static io.micronaut.http.MediaType.*;

import java.io.*;

import io.micronaut.http.annotation.*;
import io.micronaut.http.server.types.files.*;

@Controller
public class FaviconController {
	@Get("/favicon.ico")
	@Produces(APPLICATION_OCTET_STREAM)
	public StreamedFile getFavicon() {
		InputStream faviconStream = getClass().getResourceAsStream("/static/favicon.ico");
		return new StreamedFile(faviconStream, APPLICATION_OCTET_STREAM_TYPE).attach("favicon.ico");
	}
}