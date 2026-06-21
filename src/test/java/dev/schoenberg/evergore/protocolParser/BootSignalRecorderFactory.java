package dev.schoenberg.evergore.protocolParser;

import jakarta.inject.Singleton;

import io.micronaut.context.annotation.Factory;

@Factory
class BootSignalRecorderFactory {
	@Singleton
	BootSignalRecorder recorder() {
		return new BootSignalRecorder();
	}
}
