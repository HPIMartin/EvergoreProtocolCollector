package dev.schoenberg.evergore.protocolParser.rest.controller.api.wire;

import dev.schoenberg.evergore.protocolParser.businessLogic.account.Role;

public final class RoleWireNames {
	public static final String MEMBER = "member";
	public static final String ADMIN = "admin";

	private RoleWireNames() {}

	public static String of(Role role) {
		return switch (role) {
			case MEMBER -> MEMBER;
			case ADMIN -> ADMIN;
		};
	}
}
