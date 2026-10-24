package dev.schoenberg.evergore.protocolParser.helper.security;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import dev.schoenberg.evergore.protocolParser.helper.config.PasswordHashConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

@Timeout(value = 30, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
class Argon2idPasswordHasherTest {
	private static final long TEN_SECONDS = 10;
	private static final String CHOSEN = "Gilde-2026!";
	private static final String COMPOSED = "Ärger-Gilde9";
	private static final String DECOMPOSED = "A" + Character.toString(0x0308) + "rger-Gilde9";
	private static final PasswordHashConfiguration SMALL = new PasswordHashConfiguration(1024, 1, 1, 2);

	private final Argon2idPasswordHasher tested = new Argon2idPasswordHasher(SMALL, new SecureRandom());

	@Test
	void verifiesThePasswordItHashed() {
		String encoded = tested.hash(CHOSEN);

		boolean verified = tested.verify(CHOSEN, encoded);

		assertThat(verified).isTrue();
	}

	@ParameterizedTest
	@ValueSource(strings = {"Gilde-2027!", "gilde-2026!", "Gilde-2026! ", ""})
	void refusesAnyOtherPassword(String wrong) {
		String encoded = tested.hash(CHOSEN);

		boolean verified = tested.verify(wrong, encoded);

		assertThat(verified).isFalse();
	}

	@Test
	void hashesTheSamePasswordToTwoDifferentValues() {
		String first = tested.hash(CHOSEN);

		String second = tested.hash(CHOSEN);

		assertThat(first).isNotEqualTo(second);
	}

	@Test
	void verifiesBothHashesOfTheSamePassword() {
		String first = tested.hash(CHOSEN);
		String second = tested.hash(CHOSEN);

		boolean verifiedBoth = tested.verify(CHOSEN, first) && tested.verify(CHOSEN, second);

		assertThat(verifiedBoth).isTrue();
	}

	@Test
	void carriesTheAlgorithmTheParametersAndTheSaltInTheEncodedForm() {
		String encoded = tested.hash(CHOSEN);

		String[] parts = encoded.split("\\$");

		assertThat(parts).hasSize(6);
		assertThat(parts[1]).isEqualTo("argon2id");
		assertThat(parts[2]).isEqualTo("v=19");
		assertThat(parts[3]).isEqualTo("m=1024,t=1,p=1");
	}

	@Test
	void saltsEveryHashWithSixteenRandomBytes() {
		String[] parts = tested.hash(CHOSEN).split("\\$");

		byte[] salt = Base64.getDecoder().decode(parts[4]);

		assertThat(salt).hasSize(16);
	}

	@Test
	void storesAHashOfThirtyTwoBytes() {
		String[] parts = tested.hash(CHOSEN).split("\\$");

		byte[] hash = Base64.getDecoder().decode(parts[5]);

		assertThat(hash).hasSize(32);
	}

	@Test
	void keepsTheClearPasswordOutOfTheEncodedForm() {
		String encoded = tested.hash(CHOSEN);

		assertThat(encoded).doesNotContain(CHOSEN);
	}

	@Test
	void verifiesAHashMadeWithOtherParametersByTheParametersItCarries() {
		String madeWithOtherParameters = new Argon2idPasswordHasher(new PasswordHashConfiguration(2048, 2, 2, 2), new SecureRandom()).hash(CHOSEN);

		boolean verified = tested.verify(CHOSEN, madeWithOtherParameters);

		assertThat(verified).isTrue();
	}

	@Test
	void refusesTheWrongPasswordAgainstAHashMadeWithOtherParameters() {
		String madeWithOtherParameters = new Argon2idPasswordHasher(new PasswordHashConfiguration(2048, 2, 2, 2), new SecureRandom()).hash(CHOSEN);

		boolean verified = tested.verify("Gilde-2027!", madeWithOtherParameters);

		assertThat(verified).isFalse();
	}

	@ParameterizedTest
	@ValueSource(strings = {"", "garbage", "$argon2id$v=19$m=1024,t=1,p=1$c2FsdA$", "$argon2i$v=19$m=1024,t=1,p=1$c2FsdA$aGFzaA", "$argon2id$v=16$m=1024,t=1,p=1$c2FsdA$aGFzaA",
			"$argon2id$v=19$m=x,t=1,p=1$c2FsdA$aGFzaA", "$argon2id$v=19$m=1024,t=1,p=1$!!$aGFzaA", "$argon2id$v=19$m=1024,t=1,p=1$c2FsdA$a",
			"$argon2id$v=19$m=99999999999,t=1,p=1$c2FsdA$aGFzaA"})
	void refusesAnEncodedValueItCannotRead(String unreadable) {
		boolean verified = tested.verify(CHOSEN, unreadable);

		assertThat(verified).isFalse();
	}

	@Test
	void verifiesADecomposedPasswordAgainstTheHashOfTheComposedOne() {
		String encoded = tested.hash(COMPOSED);

		boolean verified = tested.verify(DECOMPOSED, encoded);

		assertThat(verified).isTrue();
	}

	@Test
	void verifiesAComposedPasswordAgainstTheHashOfTheDecomposedOne() {
		String encoded = tested.hash(DECOMPOSED);

		boolean verified = tested.verify(COMPOSED, encoded);

		assertThat(verified).isTrue();
	}

	@Test
	void letsAHashWaitForAFreePermitInsteadOfRefusingIt() throws Exception {
		Semaphore permits = new Semaphore(1);
		Argon2idPasswordHasher capped = new Argon2idPasswordHasher(SMALL, new SecureRandom(), permits);
		permits.acquire();
		ExecutorService other = Executors.newSingleThreadExecutor();
		try {
			Future<String> hashed = other.submit(() -> capped.hash(CHOSEN));
			boolean queued = awaitsAPermit(permits);
			boolean doneWhileHeld = hashed.isDone();
			permits.release();

			String encoded = hashed.get(TEN_SECONDS, TimeUnit.SECONDS);
			boolean verified = capped.verify(CHOSEN, encoded);

			assertThat(queued).isTrue();
			assertThat(doneWhileHeld).isFalse();
			assertThat(verified).isTrue();
		} finally {
			other.shutdownNow();
		}
	}

	@Test
	void letsAVerificationWaitForAFreePermitInsteadOfRefusingIt() throws Exception {
		Semaphore permits = new Semaphore(1);
		Argon2idPasswordHasher capped = new Argon2idPasswordHasher(SMALL, new SecureRandom(), permits);
		String encoded = capped.hash(CHOSEN);
		permits.acquire();
		ExecutorService other = Executors.newSingleThreadExecutor();
		try {
			Future<Boolean> verified = other.submit(() -> capped.verify(CHOSEN, encoded));
			boolean queued = awaitsAPermit(permits);
			boolean doneWhileHeld = verified.isDone();
			permits.release();

			boolean verifiedOnceFree = verified.get(TEN_SECONDS, TimeUnit.SECONDS);

			assertThat(queued).isTrue();
			assertThat(doneWhileHeld).isFalse();
			assertThat(verifiedOnceFree).isTrue();
		} finally {
			other.shutdownNow();
		}
	}

	@Test
	void givesItsPermitBackAfterEachHashAndEachVerification() {
		Semaphore permits = new Semaphore(2);
		Argon2idPasswordHasher capped = new Argon2idPasswordHasher(SMALL, new SecureRandom(), permits);

		String encoded = capped.hash(CHOSEN);
		capped.verify(CHOSEN, encoded);
		capped.verify(CHOSEN, "garbage");
		capped.verify(CHOSEN, "$argon2id$v=19$m=99999999999,t=1,p=1$c2FsdA$aGFzaA");

		assertThat(permits.availablePermits()).isEqualTo(2);
	}

	private static boolean awaitsAPermit(Semaphore permits) {
		long deadline = System.nanoTime() + Duration.ofSeconds(TEN_SECONDS).toNanos();
		while (!permits.hasQueuedThreads() && System.nanoTime() < deadline) {
			Thread.onSpinWait();
		}
		return permits.hasQueuedThreads();
	}
}
