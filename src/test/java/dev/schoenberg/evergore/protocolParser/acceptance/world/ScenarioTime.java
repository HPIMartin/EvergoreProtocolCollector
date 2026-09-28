package dev.schoenberg.evergore.protocolParser.acceptance.world;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static dev.schoenberg.evergore.protocolParser.businessLogic.Constants.APP_ZONE;

public class ScenarioTime {
	private static final LocalDateTime START = LocalDateTime.of(2026, 1, 1, 0, 0);

	private volatile Instant now = START.atZone(APP_ZONE).toInstant();

	public void setTo(LocalDateTime wallClock) {
		now = wallClock.atZone(APP_ZONE).toInstant();
	}

	public Instant now() {
		return now;
	}

	public Clock clock() {
		return new ScenarioClock(APP_ZONE);
	}

	private class ScenarioClock extends Clock {
		private final ZoneId zone;

		private ScenarioClock(ZoneId zone) {
			this.zone = zone;
		}

		@Override
		public ZoneId getZone() {
			return zone;
		}

		@Override
		public Clock withZone(ZoneId other) {
			return new ScenarioClock(other);
		}

		@Override
		public Instant instant() {
			return now;
		}
	}
}
