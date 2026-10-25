package dev.schoenberg.evergore.protocolParser.rest.filter;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

final class MutableClock extends Clock {
	private Instant now;
	private int reads;

	MutableClock(Instant start) {
		now = start;
	}

	@Override
	public Instant instant() {
		reads++;
		return now;
	}

	int reads() {
		return reads;
	}

	@Override
	public ZoneId getZone() {
		return ZoneOffset.UTC;
	}

	@Override
	public Clock withZone(ZoneId zone) {
		throw new UnsupportedOperationException();
	}

	void advanceBy(Duration duration) {
		now = now.plus(duration);
	}
}
