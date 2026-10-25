package dev.schoenberg.evergore.protocolParser.rest.controller.api;

import dev.schoenberg.evergore.protocolParser.businessLogic.account.Login;

public final class SessionCookie {
	public static final String NAME = "epc_session";

	static final String HEADER = "Set-Cookie";

	private SessionCookie() {}

	static String holding(String loginValue) {
		return NAME + "=" + loginValue + "; Max-Age=" + Login.LIFETIME.toSeconds() + "; Path=/; HttpOnly; SameSite=Lax";
	}
}
