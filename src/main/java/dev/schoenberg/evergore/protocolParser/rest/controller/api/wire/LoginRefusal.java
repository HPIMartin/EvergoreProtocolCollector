package dev.schoenberg.evergore.protocolParser.rest.controller.api.wire;

import com.fasterxml.jackson.annotation.JsonProperty;

import dev.schoenberg.evergore.protocolParser.businessLogic.account.PasswordRefusal;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.PasswordRefusal.PasswordRefusalVisitor;

public record LoginRefusal(@JsonProperty("refusal") String refusal) {
	public static final LoginRefusal WRONG_NAME_OR_PASSWORD = new LoginRefusal("wrong-name-or-password");
	public static final LoginRefusal WRONG_CURRENT_PASSWORD = new LoginRefusal("wrong-current-password");
	public static final LoginRefusal NO_ACCOUNT = new LoginRefusal("no-account");
	public static final LoginRefusal BREAKS_THE_RULE = new LoginRefusal("breaks-the-rule");
	public static final LoginRefusal SAME_AS_BEFORE = new LoginRefusal("same-as-before");

	public static LoginRefusal of(PasswordRefusal refusal) {
		return refusal.accept(new PasswordRefusalVisitor<>() {
			@Override
			public LoginRefusal breaksTheRule() {
				return BREAKS_THE_RULE;
			}

			@Override
			public LoginRefusal sameAsBefore() {
				return SAME_AS_BEFORE;
			}
		});
	}
}
