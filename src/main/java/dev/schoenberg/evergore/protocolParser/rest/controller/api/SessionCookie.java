package dev.schoenberg.evergore.protocolParser.rest.controller.api;

import dev.schoenberg.evergore.protocolParser.businessLogic.account.Login;

final class SessionCookie {
	static final String HEADER = "Set-Cookie";

	private static final String NAME = "epc_session";

	private SessionCookie() {}

	static String holding(String loginValue) {
		return NAME + "=" + loginValue + "; Max-Age=" + Login.LIFETIME.toSeconds() + "; Path=/; HttpOnly; SameSite=Lax";
	}
}
