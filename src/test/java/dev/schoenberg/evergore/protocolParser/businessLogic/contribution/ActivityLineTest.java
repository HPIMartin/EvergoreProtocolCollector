package dev.schoenberg.evergore.protocolParser.businessLogic.contribution;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static dev.schoenberg.evergore.protocolParser.businessLogic.Constants.APP_ZONE;
import static org.assertj.core.api.Assertions.assertThat;

class ActivityLineTest {
	private static final Instant NEWEST = Instant.parse("2026-01-31T11:00:00Z");

	private static Instant berlin(int year, int month, int day, int hour, int minute) {
		return ZonedDateTime.of(year, month, day, hour, minute, 0, 0, APP_ZONE).toInstant();
	}

	@Test
	void countsAMemberActiveWhoseLatestActivityIsExactlyThirtyDaysBeforeTheNewest() {
		ActivityLine tested = ActivityLine.of(Map.of("Boreas", Instant.parse("2026-01-01T11:00:00Z")), Map.of("Aurora", NEWEST));

		boolean active = tested.isActive("Boreas");

		assertThat(active).isTrue();
	}

	@Test
	void countsAMemberDormantWhoseLatestActivityIsOneMinuteBeforeTheCut() {
		ActivityLine tested = ActivityLine.of(Map.of("Calix", Instant.parse("2026-01-01T10:59:00Z")), Map.of("Aurora", NEWEST));

		boolean active = tested.isActive("Calix");

		assertThat(active).isFalse();
	}

	@Test
	void measuresThirtyDaysOnTheBerlinClockAcrossTheAutumnChange() {
		ActivityLine tested = ActivityLine.of(Map.of("Calix", berlin(2026, 10, 1, 12, 0), "Aurora", berlin(2026, 10, 31, 12, 0)), Map.of());

		boolean active = tested.isActive("Calix");

		assertThat(active).isTrue();
	}

	@Test
	void countsAMemberDormantOneMinuteBeforeThirtyBerlinDaysAcrossTheAutumnChange() {
		ActivityLine tested = ActivityLine.of(Map.of("Calix", berlin(2026, 10, 1, 11, 59), "Aurora", berlin(2026, 10, 31, 12, 0)), Map.of());

		boolean active = tested.isActive("Calix");

		assertThat(active).isFalse();
	}

	@Test
	void measuresThirtyDaysOnTheBerlinClockAcrossTheSpringChange() {
		ActivityLine tested = ActivityLine.of(Map.of("Calix", berlin(2026, 3, 1, 12, 0), "Aurora", berlin(2026, 3, 31, 12, 0)), Map.of());

		boolean active = tested.isActive("Calix");

		assertThat(active).isTrue();
	}

	@Test
	void countsAMemberDormantOneMinuteBeforeThirtyBerlinDaysAcrossTheSpringChange() {
		ActivityLine tested = ActivityLine.of(Map.of("Calix", berlin(2026, 3, 1, 11, 59), "Aurora", berlin(2026, 3, 31, 12, 0)), Map.of());

		boolean active = tested.isActive("Calix");

		assertThat(active).isFalse();
	}

	@ParameterizedTest(name = "calix={0}, newest={1}")
	@MethodSource("cutsThatFallIntoAClockChange")
	void measuresThirtyDaysOnTheBerlinClockWhenTheCutFallsIntoAClockChange(Instant calix, Instant newest) {
		ActivityLine tested = ActivityLine.of(Map.of("Calix", calix, "Aurora", newest), Map.of());

		boolean active = tested.isActive("Calix");

		assertThat(active).isTrue();
	}

	static Stream<Arguments> cutsThatFallIntoAClockChange() {
		return Stream.of(Arguments.of(berlin(2026, 10, 25, 2, 45), berlin(2026, 11, 24, 2, 30)), Arguments.of(berlin(2026, 3, 29, 3, 29), berlin(2026, 4, 28, 2, 30)));
	}

	@Test
	void countsTheMemberWhoMadeTheNewestMovementActive() {
		ActivityLine tested = ActivityLine.of(Map.of(), Map.of("Aurora", NEWEST));

		boolean active = tested.isActive("Aurora");

		assertThat(active).isTrue();
	}

	@Test
	void letsTheLaterOfAMembersTwoLedgersDecide() {
		ActivityLine tested = ActivityLine.of(Map.of("Brynja", Instant.parse("2026-01-01T00:00:00Z")), Map.of("Brynja", Instant.parse("2026-01-15T11:00:00Z"), "Aurora", NEWEST));

		boolean active = tested.isActive("Brynja");

		assertThat(active).isTrue();
	}

	@Test
	void measuresTheCutAgainstTheNewestMovementOfAnyMemberInEitherLedger() {
		ActivityLine tested = ActivityLine.of(Map.of("Boreas", Instant.parse("2026-01-01T11:00:00Z"), "Calix", Instant.parse("2026-02-15T11:00:00Z")), Map.of("Aurora", NEWEST));

		boolean active = tested.isActive("Boreas");

		assertThat(active).isFalse();
	}

	@Test
	void countsEveryoneActiveWhileTheLedgersHoldNoActivityAtAll() {
		ActivityLine tested = ActivityLine.of(Map.of(), Map.of());

		boolean active = tested.isActive("Aurora");

		assertThat(active).isTrue();
	}

	@Test
	void countsAMemberDormantWhoHasNoActivityWhileOthersHave() {
		ActivityLine tested = ActivityLine.of(Map.of(), Map.of("Aurora", NEWEST));

		boolean active = tested.isActive("Nobody");

		assertThat(active).isFalse();
	}
}
