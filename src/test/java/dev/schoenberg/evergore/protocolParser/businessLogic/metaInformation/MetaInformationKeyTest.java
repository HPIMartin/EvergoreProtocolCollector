package dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import static dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation.MetaInformationKey.getSumsRecomputedAt;
import static org.assertj.core.api.Assertions.assertThat;

class MetaInformationKeyTest {

	private static final String AVATAR = "Aurora";
	private static final Instant BERLIN_FALL_BACK_FIRST_PASS = Instant.parse("2026-10-25T00:30:00Z");
	private static final Instant BERLIN_FALL_BACK_SECOND_PASS = Instant.parse("2026-10-25T01:30:00Z");

	private final MetaInformationKey<Instant> tested = getSumsRecomputedAt(AVATAR);

	@Test
	void namesTheRecomputeInstantOfOneAvatar() {
		assertThat(tested.id).isEqualTo("sums_recomputed_at_" + AVATAR);
	}

	@Test
	void serializesARecomputeInstantAsEpochMillis() {
		assertThat(tested.serialize(Instant.parse("2026-09-09T03:12:00Z"))).isEqualTo("1788923520000");
	}

	@Test
	void readsARecomputeInstantBackFromEpochMillis() {
		assertThat(tested.deserialize("1788923520000")).isEqualTo(Instant.parse("2026-09-09T03:12:00Z"));
	}

	@Test
	void keepsBothHoursOfTheBerlinFallBackApart() {
		String firstPass = tested.serialize(BERLIN_FALL_BACK_FIRST_PASS);
		String secondPass = tested.serialize(BERLIN_FALL_BACK_SECOND_PASS);

		assertThat(firstPass).isNotEqualTo(secondPass);
		assertThat(tested.deserialize(firstPass)).isEqualTo(BERLIN_FALL_BACK_FIRST_PASS);
		assertThat(tested.deserialize(secondPass)).isEqualTo(BERLIN_FALL_BACK_SECOND_PASS);
	}

	@Test
	void readsBackEveryRecomputeInstantItWrote() {
		Instant recomputed = Instant.parse("2026-03-29T00:30:00Z");

		assertThat(tested.deserialize(tested.serialize(recomputed))).isEqualTo(recomputed);
	}
}
