package dev.schoenberg.evergore.protocolParser;

import java.security.SecureRandom;
import java.util.Arrays;

public final class FixedRandom extends SecureRandom {
	public static final String ADMIN_KEY_OF_ONES = "AQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQE";

	private static final long serialVersionUID = 1L;

	@Override
	public void nextBytes(byte[] bytes) {
		Arrays.fill(bytes, (byte) 1);
	}
}
