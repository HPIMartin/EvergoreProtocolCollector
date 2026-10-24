package dev.schoenberg.evergore.protocolParser.helper.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.Semaphore;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;

import dev.schoenberg.evergore.protocolParser.application.PasswordHasher;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.NormalForm;
import dev.schoenberg.evergore.protocolParser.helper.config.PasswordHashConfiguration;

public class Argon2idPasswordHasher implements PasswordHasher {
	private static final int SALT_BYTES = 16;
	private static final int HASH_BYTES = 32;
	private static final Pattern ENCODED = Pattern.compile("\\$argon2id\\$v=19\\$m=(\\d+),t=(\\d+),p=(\\d+)\\$([A-Za-z0-9+/]+)\\$([A-Za-z0-9+/]+)");
	private static final Base64.Encoder ENCODER = Base64.getEncoder().withoutPadding();
	private static final Base64.Decoder DECODER = Base64.getDecoder();

	private final PasswordHashConfiguration configuration;
	private final SecureRandom random;
	private final Semaphore permits;

	public Argon2idPasswordHasher(PasswordHashConfiguration configuration, SecureRandom random) {
		this(configuration, random, new Semaphore(configuration.maxConcurrent()));
	}

	Argon2idPasswordHasher(PasswordHashConfiguration configuration, SecureRandom random, Semaphore permits) {
		this.configuration = configuration;
		this.random = random;
		this.permits = permits;
	}

	@Override
	public String hash(String password) {
		byte[] salt = new byte[SALT_BYTES];
		random.nextBytes(salt);
		byte[] hash = argon2id(password, salt, configuration.memoryKibibytes(), configuration.iterations(), configuration.parallelism());

		return "$argon2id$v=19$m=" + configuration.memoryKibibytes() + ",t=" + configuration.iterations() + ",p=" + configuration.parallelism() + "$" + ENCODER.encodeToString(salt)
				+ "$" + ENCODER.encodeToString(hash);
	}

	@Override
	public boolean verify(String password, String encoded) {
		Matcher parts = ENCODED.matcher(encoded);
		if (!parts.matches()) {
			return false;
		}
		try {
			byte[] salt = DECODER.decode(parts.group(4));
			byte[] expected = DECODER.decode(parts.group(5));
			byte[] actual = argon2id(password, salt, Integer.parseInt(parts.group(1)), Integer.parseInt(parts.group(2)), Integer.parseInt(parts.group(3)));

			return MessageDigest.isEqual(expected, actual);
		} catch (IllegalArgumentException unreadable) {
			return false;
		}
	}

	private byte[] argon2id(String password, byte[] salt, int memoryKibibytes, int iterations, int parallelism) {
		Argon2Parameters parameters = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
				.withVersion(Argon2Parameters.ARGON2_VERSION_13)
				.withMemoryAsKB(memoryKibibytes)
				.withIterations(iterations)
				.withParallelism(parallelism)
				.withSalt(salt)
				.build();
		Argon2BytesGenerator generator = new Argon2BytesGenerator();
		generator.init(parameters);
		byte[] hash = new byte[HASH_BYTES];
		byte[] secret = NormalForm.of(password).getBytes(StandardCharsets.UTF_8);
		permits.acquireUninterruptibly();
		try {
			generator.generateBytes(secret, hash);
		} finally {
			permits.release();
		}
		return hash;
	}
}
