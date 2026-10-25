package dev.schoenberg.evergore.protocolParser.helper.security;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.Verification;

import dev.schoenberg.evergore.protocolParser.application.SignedLogins;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.Login;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.LoginSource;
import dev.schoenberg.evergore.protocolParser.businessLogic.account.Role;

public class JwtLogins implements SignedLogins {
	private static final int KEY_BYTES = 32;
	private static final String ROLE = "role";
	private static final String GENERATION = "gen";
	private static final String SOURCE = "src";

	private final Algorithm algorithm;
	private final Clock clock;

	public JwtLogins(byte[] key, Clock clock) {
		this.algorithm = Algorithm.HMAC256(key);
		this.clock = clock;
	}

	public static JwtLogins withARandomKey(SecureRandom random, Clock clock) {
		byte[] key = new byte[KEY_BYTES];
		random.nextBytes(key);
		return new JwtLogins(key, clock);
	}

	@Override
	public String issue(String accountName, Role role, long generation) {
		return signed(accountName, role, generation, LoginSource.ACCOUNT);
	}

	@Override
	public String issueForTheAdminKey(String name) {
		return signed(name, Role.ADMIN, 0, LoginSource.ADMIN_KEY);
	}

	private String signed(String name, Role role, long generation, LoginSource source) {
		Instant made = clock.instant();

		return JWT
				.create()
				.withSubject(name)
				.withClaim(ROLE, role.name())
				.withClaim(GENERATION, generation)
				.withClaim(SOURCE, source.name())
				.withJWTId(UUID.randomUUID().toString())
				.withIssuedAt(made)
				.withExpiresAt(made.plus(Login.LIFETIME))
				.sign(algorithm);
	}

	@Override
	public Optional<Login> read(String value) {
		try {
			return loginOf(verifier().verify(value));
		} catch (JWTVerificationException | IllegalArgumentException unreadable) {
			return Optional.empty();
		}
	}

	private JWTVerifier verifier() {
		Verification required = JWT
				.require(algorithm)
				.withClaimPresence("sub")
				.withClaimPresence(ROLE)
				.withClaimPresence(GENERATION)
				.withClaimPresence(SOURCE)
				.withClaimPresence("jti")
				.withClaimPresence("iat")
				.withClaimPresence("exp")
				.ignoreIssuedAt();

		return ((JWTVerifier.BaseVerification) required).build(clock);
	}

	private static Optional<Login> loginOf(DecodedJWT decoded) {
		String role = decoded.getClaim(ROLE).asString();
		String source = decoded.getClaim(SOURCE).asString();
		Long generation = decoded.getClaim(GENERATION).asLong();
		if (role == null || source == null || generation == null) {
			return Optional.empty();
		}
		return Optional
				.of(new Login(decoded.getSubject(), Role.valueOf(role), generation, LoginSource.valueOf(source), decoded.getId(), decoded.getIssuedAtAsInstant(),
						decoded.getExpiresAtAsInstant()));
	}
}
