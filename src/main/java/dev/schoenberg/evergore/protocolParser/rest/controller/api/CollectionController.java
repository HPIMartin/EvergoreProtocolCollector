package dev.schoenberg.evergore.protocolParser.rest.controller.api;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Produces;

import dev.schoenberg.evergore.protocolParser.application.Collections;
import dev.schoenberg.evergore.protocolParser.application.StartOutcome;
import dev.schoenberg.evergore.protocolParser.rest.controller.api.wire.Refusal;

import static io.micronaut.http.MediaType.APPLICATION_JSON;

@Controller(CollectionController.PATH)
public class CollectionController {
	public static final String PATH = "/api/v1/admin/collection";

	private final Collections collections;

	public CollectionController(Collections collections) {
		this.collections = collections;
	}

	@Post
	@Produces(APPLICATION_JSON)
	public HttpResponse<Refusal> start() {
		return switch (collections.startByHand()) {
			case StartOutcome.Started started -> HttpResponse.accepted();
			case StartOutcome.AlreadyRunning running -> HttpResponse.<Refusal>status(HttpStatus.CONFLICT).body(Refusal.RUNNING);
			case StartOutcome.TooEarly tooEarly -> HttpResponse.<Refusal>status(HttpStatus.CONFLICT).body(Refusal.TOO_EARLY);
		};
	}
}
