package dev.schoenberg.evergore.protocolParser.dataExtraction;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import dev.schoenberg.evergore.protocolParser.LoggerSpy;
import dev.schoenberg.evergore.protocolParser.application.Collections;
import dev.schoenberg.evergore.protocolParser.application.GuildCollection;
import dev.schoenberg.evergore.protocolParser.helper.config.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EvergoreDataCollectorJobTest {

	private static final Instant FIXED_NOW = Instant.parse("2026-06-21T12:00:00Z");

	@Test
	void restoresInterruptFlagWhenInitialDelayIsInterrupted() {
		GuildCollection collection = new GuildCollection(null, null, null, () -> {}, null, Clock.fixed(FIXED_NOW, ZoneOffset.UTC), new LoggerSpy());
		Collections collections = new Collections(collection, new LoggerSpy());
		EvergoreDataCollectorJob interruptibleJob = new EvergoreDataCollectorJob(new PositiveDelayConfiguration(), collections);
		Thread.currentThread().interrupt();
		try {
			assertThatThrownBy(() -> interruptibleJob.scheduleEvery24Hours()).isInstanceOf(RuntimeException.class);

			assertThat(Thread.currentThread().isInterrupted()).isTrue();
		} finally {
			Thread.interrupted();
		}
	}

	private static class PositiveDelayConfiguration extends Configuration {
		@Override
		public int getCollectorInitialDelaySeconds() {
			return 5;
		}
	}
}
