package dev.schoenberg.evergore.protocolParser.dataExtraction.website;

import java.time.Duration;

import org.openqa.selenium.support.ui.Sleeper;

final class CountingSleeper implements Sleeper {
	private final MutableClock clock;
	private int callCount;

	CountingSleeper(MutableClock clock) {
		this.clock = clock;
	}

	@Override
	public void sleep(Duration duration) {
		callCount++;
		clock.advanceBy(duration);
	}

	int callCount() {
		return callCount;
	}
}
