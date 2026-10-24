package dev.schoenberg.evergore.protocolParser.application;

public interface PasswordHasher {
	String hash(String password);

	boolean verify(String password, String encoded);
}
