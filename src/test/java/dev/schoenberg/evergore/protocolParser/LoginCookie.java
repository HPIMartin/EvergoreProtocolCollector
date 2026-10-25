package dev.schoenberg.evergore.protocolParser;

import io.micronaut.runtime.server.EmbeddedServer;

import dev.schoenberg.evergore.protocolParser.application.SignedLogins;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.Role;

public final class LoginCookie {
	public static final String NAME = "epc_session";

	private LoginCookie() {}

	public static String valueFor(EmbeddedServer server) {
		return server.getApplicationContext().getBean(SignedLogins.class).issueForTheAdminKey("admin");
	}

	public static String memberHeaderFor(EmbeddedServer server, String avatar) {
		return NAME + "=" + server.getApplicationContext().getBean(SignedLogins.class).issue(avatar, Role.MEMBER, 0);
	}

	public static String headerFor(EmbeddedServer server) {
		return NAME + "=" + valueFor(server);
	}
}
