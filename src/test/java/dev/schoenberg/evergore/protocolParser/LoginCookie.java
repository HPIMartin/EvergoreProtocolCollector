package dev.schoenberg.evergore.protocolParser;

import io.micronaut.runtime.server.EmbeddedServer;

import dev.schoenberg.evergore.protocolParser.application.SignedLogins;

public final class LoginCookie {
	public static final String NAME = "epc_session";

	private LoginCookie() {}

	public static String valueFor(EmbeddedServer server) {
		return server.getApplicationContext().getBean(SignedLogins.class).issueForTheAdminKey("admin");
	}

	public static String headerFor(EmbeddedServer server) {
		return NAME + "=" + valueFor(server);
	}
}
