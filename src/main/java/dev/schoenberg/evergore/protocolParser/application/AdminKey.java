package dev.schoenberg.evergore.protocolParser.application;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import dev.schoenberg.evergore.protocolParser.Logger;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.LoginName;

import static java.nio.charset.StandardCharsets.UTF_8;

public class AdminKey {
	public static final String NAME = "admin";

	private static final int KEY_BYTES = 32;
	private static final String ANNOUNCEMENT = "Console admin key, valid until the next restart: ";

	private final String key;

	private AdminKey(String key) {
		this.key = key;
	}

	public static AdminKey drawn(SecureRandom random) {
		byte[] drawn = new byte[KEY_BYTES];
		random.nextBytes(drawn);
		return new AdminKey(Base64.getUrlEncoder().withoutPadding().encodeToString(drawn));
	}

	public void announceTo(Logger logger) {
		logger.info(ANNOUNCEMENT + key);
	}

	public boolean isNamedBy(String name) {
		return new LoginName(name).key().equals(NAME);
	}

	public boolean opens(String name, String password) {
		return isNamedBy(name) && MessageDigest.isEqual(password.getBytes(UTF_8), key.getBytes(UTF_8));
	}
}
