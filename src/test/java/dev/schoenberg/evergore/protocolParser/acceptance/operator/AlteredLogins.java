package dev.schoenberg.evergore.protocolParser.acceptance.operator;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.security.GeneralSecurityException;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import static java.nio.charset.StandardCharsets.UTF_8;

public final class AlteredLogins {
	private static final ObjectMapper JSON = new ObjectMapper();
	private static final Base64.Decoder DECODER = Base64.getUrlDecoder();
	private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
	private static final byte[] ANOTHER_KEY = "a key that only someone other than the service holds".getBytes(UTF_8);

	private AlteredLogins() {}

	public static String claimingTheAdminsRole(String login) {
		return withClaim(login, "role", "ADMIN");
	}

	public static String namingAnotherAccount(String login) {
		return withClaim(login, "sub", "Boreas");
	}

	public static String signedBySomeoneOtherThanTheService(String login) {
		String signed = headerAndPayloadOf(login);
		return signed + "." + ENCODER.encodeToString(hmacOf(signed));
	}

	public static String withoutASignature(String login) {
		return headerAndPayloadOf(login) + ".";
	}

	private static String withClaim(String login, String claim, String value) {
		String[] parts = login.split("\\.");
		ObjectNode payload = payloadOf(parts[1]);
		payload.put(claim, value);
		return parts[0] + "." + ENCODER.encodeToString(payload.toString().getBytes(UTF_8)) + "." + parts[2];
	}

	private static ObjectNode payloadOf(String encoded) {
		try {
			return (ObjectNode) JSON.readTree(DECODER.decode(encoded));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static String headerAndPayloadOf(String login) {
		String[] parts = login.split("\\.");
		return parts[0] + "." + parts[1];
	}

	private static byte[] hmacOf(String signed) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(ANOTHER_KEY, "HmacSHA256"));
			return mac.doFinal(signed.getBytes(UTF_8));
		} catch (GeneralSecurityException e) {
			throw new IllegalStateException(e);
		}
	}
}
