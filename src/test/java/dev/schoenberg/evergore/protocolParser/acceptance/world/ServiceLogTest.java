package dev.schoenberg.evergore.protocolParser.acceptance.world;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ServiceLogTest {
	private static final String ANNOUNCEMENT = "Console admin key, valid until the next restart: ";

	private final ServiceLog tested = new ServiceLog();

	@Test
	void namesTheAdminKeyTheServiceAnnounced() {
		tested.write("Starting");
		tested.write(ANNOUNCEMENT + "first-key");

		String key = tested.adminKeyNamed();

		assertThat(key).isEqualTo("first-key");
	}

	@Test
	void namesTheKeyOfTheLatestStartWhenTheServiceWasStartedMoreThanOnce() {
		tested.write(ANNOUNCEMENT + "first-key");
		tested.write("Scheduled extraction finished!");
		tested.write(ANNOUNCEMENT + "second-key");

		String key = tested.adminKeyNamed();

		assertThat(key).isEqualTo("second-key");
	}

	@Test
	void failsWhenTheServiceNamedNoKey() {
		tested.write("Starting");

		assertThatThrownBy(tested::adminKeyNamed).isInstanceOf(AssertionError.class);
	}
}
