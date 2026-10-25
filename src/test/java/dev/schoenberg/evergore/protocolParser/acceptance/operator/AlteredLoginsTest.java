package dev.schoenberg.evergore.protocolParser.acceptance.operator;

import java.util.Base64;

import org.junit.jupiter.api.Test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

class AlteredLoginsTest {
	private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
	private static final String HEADER = ENCODER.encodeToString("{\"alg\":\"HS256\"}".getBytes(UTF_8));
	private static final String PAYLOAD = ENCODER.encodeToString("{\"sub\":\"Aurora\",\"role\":\"MEMBER\"}".getBytes(UTF_8));
	private static final String SIGNATURE = "the-services-signature";
	private static final String LOGIN = HEADER + "." + PAYLOAD + "." + SIGNATURE;

	@Test
	void claimsTheAdminsRoleAndKeepsTheSignatureOfTheOriginal() {
		String altered = AlteredLogins.claimingTheAdminsRole(LOGIN);

		assertThat(payloadOf(altered)).isEqualTo("{\"sub\":\"Aurora\",\"role\":\"ADMIN\"}");
		assertThat(altered).endsWith("." + SIGNATURE);
	}

	@Test
	void namesAnotherAccountAndKeepsTheSignatureOfTheOriginal() {
		String altered = AlteredLogins.namingAnotherAccount(LOGIN);

		assertThat(payloadOf(altered)).isEqualTo("{\"sub\":\"Boreas\",\"role\":\"MEMBER\"}");
		assertThat(altered).endsWith("." + SIGNATURE);
	}

	@Test
	void signsTheSameHeaderAndPayloadWithAnotherSignature() {
		String altered = AlteredLogins.signedBySomeoneOtherThanTheService(LOGIN);

		assertThat(altered).startsWith(HEADER + "." + PAYLOAD + ".").isNotEqualTo(LOGIN);
		assertThat(altered.split("\\.")).hasSize(3);
	}

	@Test
	void dropsTheSignature() {
		String altered = AlteredLogins.withoutASignature(LOGIN);

		assertThat(altered).isEqualTo(HEADER + "." + PAYLOAD + ".");
	}

	private static String payloadOf(String login) {
		return new String(Base64.getUrlDecoder().decode(login.split("\\.")[1]), UTF_8);
	}
}
