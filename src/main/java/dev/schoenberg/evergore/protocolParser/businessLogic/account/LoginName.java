package dev.schoenberg.evergore.protocolParser.businessLogic.account;

import java.util.Locale;

public record LoginName(String typed) {
	private static final String SURROUNDING_SPACE = "^[\\p{Z}\\s]+|[\\p{Z}\\s]+$";

	public String key() {
		return NormalForm.of(typed).replaceAll(SURROUNDING_SPACE, "").toLowerCase(Locale.ROOT);
	}

	public boolean matches(String avatar) {
		return key().equals(new LoginName(avatar).key());
	}
}
