package dev.schoenberg.evergore.protocolParser.helper.security;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Optional;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import dev.schoenberg.evergore.protocolParser.businessLogic.account.Login;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.Role;

import static org.assertj.core.api.Assertions.assertThat;

class JwtLoginsTest {
	private static final Instant MADE = Instant.parse("2026-01-01T12:00:00Z");
	private static final byte[] KEY = bytesOf(1);
	private static final byte[] ANOTHER_KEY = bytesOf(2);
	private static final Base64.Encoder URL = Base64.getUrlEncoder().withoutPadding();
	private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();

	private final MovableClock clock = new MovableClock(MADE);
	private final JwtLogins tested = new JwtLogins(KEY, clock);

	@Test
	void readsBackTheNameTheRoleAndTheGenerationOfTheLoginItIssued() {
		String value = tested.issue("aurora", Role.ADMIN, 7);

		Optional<Login> login = tested.read(value);

		assertThat(login).hasValueSatisfying(read -> {
			assertThat(read.accountName()).isEqualTo("aurora");
			assertThat(read.role()).isEqualTo(Role.ADMIN);
			assertThat(read.generation()).isEqualTo(7);
		});
	}

	@Test
	void takesTheTimeItWasMadeFromTheClock() {
		String value = tested.issue("aurora", Role.MEMBER, 0);

		Optional<Instant> issuedAt = tested.read(value).map(Login::issuedAt);

		assertThat(issuedAt).contains(MADE);
	}

	@Test
	void endsTheLoginExactlySevenDaysAfterItWasMade() {
		String value = tested.issue("aurora", Role.MEMBER, 0);

		Optional<Instant> expiresAt = tested.read(value).map(Login::expiresAt);

		assertThat(expiresAt).contains(MADE.plus(Duration.ofDays(7)));
	}

	@Test
	void stillReadsTheLoginOneSecondBeforeItEnds() {
		String value = tested.issue("aurora", Role.MEMBER, 0);
		clock.setTo(MADE.plus(Duration.ofDays(7)).minusSeconds(1));

		Optional<Login> login = tested.read(value);

		assertThat(login).isPresent();
	}

	@Test
	void stillReadsTheLoginWhenTheClockStepsBackThirtySecondsAfterItWasMade() {
		String value = tested.issue("aurora", Role.MEMBER, 0);
		clock.setTo(MADE.minusSeconds(30));

		Optional<Login> login = tested.read(value);

		assertThat(login).isPresent();
	}

	@Test
	void refusesTheLoginAtTheMomentItEnds() {
		String value = tested.issue("aurora", Role.MEMBER, 0);
		clock.setTo(MADE.plus(Duration.ofDays(7)));

		Optional<Login> login = tested.read(value);

		assertThat(login).isEmpty();
	}

	@Test
	void refusesTheLoginLongAfterItEnded() {
		String value = tested.issue("aurora", Role.MEMBER, 0);
		clock.setTo(MADE.plus(Duration.ofDays(30)));

		Optional<Login> login = tested.read(value);

		assertThat(login).isEmpty();
	}

	@Test
	void doesNotExtendTheLoginWhenItIsRead() {
		String value = tested.issue("aurora", Role.MEMBER, 0);
		clock.setTo(MADE.plus(Duration.ofDays(6)));
		tested.read(value);
		clock.setTo(MADE.plus(Duration.ofDays(7)));

		Optional<Login> login = tested.read(value);

		assertThat(login).isEmpty();
	}

	@Test
	void givesEveryLoginAUniqueId() {
		String first = tested.issue("aurora", Role.MEMBER, 0);
		String second = tested.issue("aurora", Role.MEMBER, 0);

		Optional<String> firstId = tested.read(first).map(Login::id);
		Optional<String> secondId = tested.read(second).map(Login::id);

		assertThat(firstId).isPresent().isNotEqualTo(secondId);
	}

	@Test
	void refusesALoginSignedWithAnotherKey() {
		String value = new JwtLogins(ANOTHER_KEY, clock).issue("aurora", Role.ADMIN, 0);

		Optional<Login> login = tested.read(value);

		assertThat(login).isEmpty();
	}

	@Test
	void refusesALoginOfAnotherInstanceThatDrewItsOwnKey() {
		String value = JwtLogins.withARandomKey(new SecureRandom(), clock).issue("aurora", Role.ADMIN, 0);

		Optional<Login> login = JwtLogins.withARandomKey(new SecureRandom(), clock).read(value);

		assertThat(login).isEmpty();
	}

	@Test
	void readsTheLoginOfTheInstanceThatDrewTheKey() {
		JwtLogins drawn = JwtLogins.withARandomKey(new SecureRandom(), clock);
		String value = drawn.issue("aurora", Role.MEMBER, 3);

		Optional<Login> login = drawn.read(value);

		assertThat(login).map(Login::generation).contains(3L);
	}

	@Test
	void refusesALoginWhoseHeaderNamesNoAlgorithm() {
		String value = unsigned("{\"alg\":\"none\",\"typ\":\"JWT\"}", validClaims("aurora", "ADMIN"));

		Optional<Login> login = tested.read(value);

		assertThat(login).isEmpty();
	}

	@Test
	void refusesALoginWithoutASignature() {
		String signed = tested.issue("aurora", Role.MEMBER, 0);
		String withoutSignature = signed.substring(0, signed.lastIndexOf('.') + 1);

		Optional<Login> login = tested.read(withoutSignature);

		assertThat(login).isEmpty();
	}

	@Test
	void refusesALoginWhoseHeaderNamesNoneButWhoseSignatureIsTheRealOne() {
		String signed = tested.issue("aurora", Role.MEMBER, 0);
		String[] parts = signed.split("\\.");
		String forged = URL.encodeToString("{\"alg\":\"none\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8)) + "." + parts[1] + "." + parts[2];

		Optional<Login> login = tested.read(forged);

		assertThat(login).isEmpty();
	}

	@ParameterizedTest
	@ValueSource(strings = {"HS384", "HS512"})
	void refusesALoginSignedWithAnotherAlgorithmByTheRightKey(String algorithm) {
		Algorithm other = algorithm.equals("HS384") ? Algorithm.HMAC384(KEY) : Algorithm.HMAC512(KEY);
		String value = JWT
				.create()
				.withSubject("aurora")
				.withClaim("role", "ADMIN")
				.withClaim("gen", 0L)
				.withJWTId("an-id")
				.withIssuedAt(MADE)
				.withExpiresAt(MADE.plus(Duration.ofDays(7)))
				.sign(other);

		Optional<Login> login = tested.read(value);

		assertThat(login).isEmpty();
	}

	@Test
	void refusesALoginWhoseNameWasAltered() {
		String signed = tested.issue("aurora", Role.MEMBER, 0);

		Optional<Login> login = tested.read(withPayloadReplaced(signed, "\"sub\":\"aurora\"", "\"sub\":\"boreas\""));

		assertThat(login).isEmpty();
	}

	@Test
	void refusesALoginWhoseRoleWasAltered() {
		String signed = tested.issue("aurora", Role.MEMBER, 0);

		Optional<Login> login = tested.read(withPayloadReplaced(signed, "\"role\":\"MEMBER\"", "\"role\":\"ADMIN\""));

		assertThat(login).isEmpty();
	}

	@Test
	void refusesALoginWhoseGenerationWasAltered() {
		String signed = tested.issue("aurora", Role.MEMBER, 4);

		Optional<Login> login = tested.read(withPayloadReplaced(signed, "\"gen\":4", "\"gen\":5"));

		assertThat(login).isEmpty();
	}

	@Test
	void refusesALoginWhoseExpiryWasPushedOut() {
		String signed = tested.issue("aurora", Role.MEMBER, 0);
		long pushedOut = MADE.plus(Duration.ofDays(70)).getEpochSecond();

		Optional<Login> login = tested.read(withPayloadReplaced(signed, "\"exp\":" + MADE.plus(Duration.ofDays(7)).getEpochSecond(), "\"exp\":" + pushedOut));

		assertThat(login).isEmpty();
	}

	@Test
	void refusesALoginSignedByTheRightKeyWhoseRoleIsNoString() {
		String value = claimsOfAValidLogin().withClaim("role", 5).sign(Algorithm.HMAC256(KEY));

		Optional<Login> login = tested.read(value);

		assertThat(login).isEmpty();
	}

	@Test
	void refusesALoginSignedByTheRightKeyWhoseGenerationIsNoNumber() {
		String value = JWT
				.create()
				.withSubject("aurora")
				.withClaim("role", "ADMIN")
				.withClaim("gen", "zero")
				.withJWTId("an-id")
				.withIssuedAt(MADE)
				.withExpiresAt(MADE.plus(Duration.ofDays(7)))
				.sign(Algorithm.HMAC256(KEY));

		Optional<Login> login = tested.read(value);

		assertThat(login).isEmpty();
	}

	@ParameterizedTest
	@ValueSource(strings = {"ROOT", "admin", ""})
	void refusesALoginSignedByTheRightKeyWhoseRoleNamesNoRole(String role) {
		String value = claimsOfAValidLogin().withClaim("role", role).sign(Algorithm.HMAC256(KEY));

		Optional<Login> login = tested.read(value);

		assertThat(login).isEmpty();
	}

	@ParameterizedTest
	@ValueSource(strings = {"", " ", "garbage", "a.b", "a.b.c", "...", "eyJhbGciOiJIUzI1NiJ9.e30.", "not a jwt at all.really.no"})
	void refusesAValueThatIsNoLogin(String value) {
		Optional<Login> login = tested.read(value);

		assertThat(login).isEmpty();
	}

	private static JWTCreator.Builder claimsOfAValidLogin() {
		return JWT.create().withSubject("aurora").withClaim("gen", 0L).withJWTId("an-id").withIssuedAt(MADE).withExpiresAt(MADE.plus(Duration.ofDays(7)));
	}

	private static String unsigned(String header, String claims) {
		return URL.encodeToString(header.getBytes(StandardCharsets.UTF_8)) + "." + URL.encodeToString(claims.getBytes(StandardCharsets.UTF_8)) + ".";
	}

	private static String validClaims(String name, String role) {
		return "{\"sub\":\"" + name + "\",\"role\":\"" + role + "\",\"gen\":0,\"jti\":\"an-id\",\"iat\":" + MADE.getEpochSecond() + ",\"exp\":"
				+ MADE.plus(Duration.ofDays(7)).getEpochSecond() + "}";
	}

	private static String withPayloadReplaced(String signed, String from, String to) {
		String[] parts = signed.split("\\.");
		String payload = new String(URL_DECODER.decode(parts[1]), StandardCharsets.UTF_8);
		assertThat(payload).contains(from);

		return parts[0] + "." + URL.encodeToString(payload.replace(from, to).getBytes(StandardCharsets.UTF_8)) + "." + parts[2];
	}

	private static byte[] bytesOf(int fill) {
		byte[] bytes = new byte[32];
		java.util.Arrays.fill(bytes, (byte) fill);
		return bytes;
	}

	private static class MovableClock extends Clock {
		private Instant now;

		MovableClock(Instant now) {
			this.now = now;
		}

		void setTo(Instant instant) {
			now = instant;
		}

		@Override
		public ZoneId getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(ZoneId zone) {
			return this;
		}

		@Override
		public Instant instant() {
			return now;
		}
	}
}
