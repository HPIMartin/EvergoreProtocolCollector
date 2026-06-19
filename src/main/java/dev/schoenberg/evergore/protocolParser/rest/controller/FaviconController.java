package dev.schoenberg.evergore.protocolParser.rest.controller;

import java.io.*;

import io.micronaut.http.annotation.*;
import io.micronaut.http.server.types.files.*;

import static io.micronaut.http.MediaType.*;

@Controller
public class FaviconController {
	public static final String PATH = "/favicon.ico";

	@Get(PATH)
	@Produces(APPLICATION_OCTET_STREAM)
	public StreamedFile getFavicon() {
		InputStream faviconStream = getClass().getResourceAsStream("/static/favicon.ico");
		return new StreamedFile(faviconStream, APPLICATION_OCTET_STREAM_TYPE).attach("favicon.ico");
	}
}
