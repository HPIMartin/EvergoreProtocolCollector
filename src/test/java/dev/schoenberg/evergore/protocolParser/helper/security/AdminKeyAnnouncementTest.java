package dev.schoenberg.evergore.protocolParser.helper.security;

import org.junit.jupiter.api.Test;

import dev.schoenberg.evergore.protocolParser.FixedRandom;
import dev.schoenberg.evergore.protocolParser.LoggerSpy;
import dev.schoenberg.evergore.protocolParser.application.AdminKey;

import static org.assertj.core.api.Assertions.assertThat;

class AdminKeyAnnouncementTest {
	private final LoggerSpy logger = new LoggerSpy();
	private final AdminKeyAnnouncement tested = new AdminKeyAnnouncement(AdminKey.drawn(new FixedRandom()), logger);

	@Test
	void logsTheAdminKeyOnceWhenTheServiceStarts() {
		tested.onApplicationEvent(null);

		assertThat(logger.infoMessages()).containsExactly("Console admin key, valid until the next restart: " + FixedRandom.ADMIN_KEY_OF_ONES);
	}
}
