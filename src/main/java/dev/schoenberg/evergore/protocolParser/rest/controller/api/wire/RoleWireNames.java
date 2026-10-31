package dev.schoenberg.evergore.protocolParser.rest.controller.api.wire;

import java.util.Optional;

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

	public static Optional<Role> parse(Object wireValue) {
		if (!(wireValue instanceof String wireName)) {
			return Optional.empty();
		}
		return switch (wireName) {
			case MEMBER -> Optional.of(Role.MEMBER);
			case ADMIN -> Optional.of(Role.ADMIN);
			default -> Optional.empty();
		};
	}
}
