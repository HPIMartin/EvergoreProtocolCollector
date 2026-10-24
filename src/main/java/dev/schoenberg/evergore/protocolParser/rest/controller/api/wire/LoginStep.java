package dev.schoenberg.evergore.protocolParser.rest.controller.api.wire;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LoginStep(@JsonProperty("step") String step) {
	public static final LoginStep LOGGED_IN = new LoginStep("logged-in");
	public static final LoginStep CHOOSE_NEW_PASSWORD = new LoginStep("choose-new-password");
}
