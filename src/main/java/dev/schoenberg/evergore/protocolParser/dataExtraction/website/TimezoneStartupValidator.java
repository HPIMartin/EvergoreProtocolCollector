package dev.schoenberg.evergore.protocolParser.dataExtraction.website;

import java.time.ZoneId;

import jakarta.inject.Singleton;

import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.context.event.StartupEvent;

import dev.schoenberg.evergore.protocolParser.Logger;

@Singleton
public class TimezoneStartupValidator implements ApplicationEventListener<StartupEvent> {
	private final ZoneId zone;
	private final Logger logger;

	public TimezoneStartupValidator(ZoneId zone, Logger logger) {
		this.zone = zone;
		this.logger = logger;
	}

	@Override
	public void onApplicationEvent(StartupEvent event) {
		validateTimezone();
	}

	void validateTimezone() {
		if (!zone.getRules().isFixedOffset()) {
			String reason = "Effective timezone '" + zone.getId() + "' observes daylight saving and is not supported. Set a fixed-offset zone (e.g. UTC).";
			logger.error(reason);
			throw new IllegalStateException(reason);
		}
	}
}
