package dev.schoenberg.evergore.protocolParser.rest.controller.api.wire;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record LoginStep(@JsonProperty("step") String step, @JsonProperty("next") String next) {
	public static final LoginStep CHOOSE_NEW_PASSWORD = new LoginStep("choose-new-password", null);

	public static LoginStep loggedInTo(String next) {
		return new LoginStep("logged-in", next);
	}
}
