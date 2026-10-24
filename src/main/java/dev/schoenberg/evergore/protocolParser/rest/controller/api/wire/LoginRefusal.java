package dev.schoenberg.evergore.protocolParser.rest.controller.api.wire;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LoginRefusal(@JsonProperty("refusal") String refusal) {
	public static final LoginRefusal WRONG_NAME_OR_PASSWORD = new LoginRefusal("wrong-name-or-password");
}
