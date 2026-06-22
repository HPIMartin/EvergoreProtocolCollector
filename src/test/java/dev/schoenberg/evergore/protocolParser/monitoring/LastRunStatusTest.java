package dev.schoenberg.evergore.protocolParser.monitoring;

import java.time.*;

import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.*;

class LastRunStatusTest {

	private LastRunStatus tested;

	@BeforeEach
	void setup() {
		tested = new LastRunStatus();
	}

	@Test
	void isEmptyInitially() {
		assertThat(tested.lastSuccessfulRun()).isEmpty();
	}

	@Test
	void returnsTheRecordedInstant() {
		Instant recorded = Instant.parse("2026-06-21T10:00:00Z");

		tested.recordSuccessfulRun(recorded);

		assertThat(tested.lastSuccessfulRun()).contains(recorded);
	}

	@Test
	void secondRecordOverwritesFirst() {
		Instant first = Instant.parse("2026-06-21T08:00:00Z");
		Instant second = Instant.parse("2026-06-21T10:00:00Z");

		tested.recordSuccessfulRun(first);
		tested.recordSuccessfulRun(second);

		assertThat(tested.lastSuccessfulRun()).contains(second);
	}
}
