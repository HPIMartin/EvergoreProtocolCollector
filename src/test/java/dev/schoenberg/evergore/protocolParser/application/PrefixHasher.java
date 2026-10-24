package dev.schoenberg.evergore.protocolParser.application;

import java.util.List;

class PrefixHasher implements PasswordHasher {
	private final List<String> trail;
	private int verifications;
	private Runnable duringTheNextVerification = () -> {};

	PrefixHasher(List<String> trail) {
		this.trail = trail;
	}

	void runDuringTheNextVerification(Runnable action) {
		duringTheNextVerification = action;
	}

	int verifications() {
		return verifications;
	}

	@Override
	public String hash(String clear) {
		trail.add("hash");
		return "hashed:" + clear;
	}

	@Override
	public boolean verify(String clear, String encoded) {
		trail.add("verify");
		verifications++;
		Runnable action = duringTheNextVerification;
		duringTheNextVerification = () -> {};
		action.run();
		return encoded.equals("hashed:" + clear);
	}
}
