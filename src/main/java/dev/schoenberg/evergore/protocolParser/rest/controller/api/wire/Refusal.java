package dev.schoenberg.evergore.protocolParser.rest.controller.api.wire;

import com.fasterxml.jackson.annotation.JsonProperty;

import dev.schoenberg.evergore.protocolParser.businessLogic.account.PasswordRefusal;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.PasswordRefusal.PasswordRefusalVisitor;

public record Refusal(@JsonProperty("refusal") String refusal) {
	public static final Refusal WRONG_NAME_OR_PASSWORD = new Refusal("wrong-name-or-password");
	public static final Refusal WRONG_CURRENT_PASSWORD = new Refusal("wrong-current-password");
	public static final Refusal RESTING = new Refusal("resting");
	public static final Refusal NO_ACCOUNT = new Refusal("no-account");
	public static final Refusal BREAKS_THE_RULE = new Refusal("breaks-the-rule");
	public static final Refusal SAME_AS_BEFORE = new Refusal("same-as-before");
	public static final Refusal UNKNOWN_ROLE = new Refusal("unknown-role");
	public static final Refusal OWN_ADMIN_ROLE = new Refusal("own-admin-role");
	public static final Refusal RUNNING = new Refusal("running");
	public static final Refusal TOO_EARLY = new Refusal("too-early");

	public static Refusal of(PasswordRefusal refusal) {
		return refusal.accept(new PasswordRefusalVisitor<>() {
			@Override
			public Refusal breaksTheRule() {
				return BREAKS_THE_RULE;
			}

			@Override
			public Refusal sameAsBefore() {
				return SAME_AS_BEFORE;
			}
		});
	}
}
