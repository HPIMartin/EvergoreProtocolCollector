package dev.schoenberg.evergore.protocolParser;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;

import io.micronaut.runtime.*;
import io.micronaut.test.extensions.junit5.annotation.*;
import jakarta.inject.*;

@MicronautTest
public class SmokeTest {
	@Inject
	private EmbeddedApplication<?> application;

	@Test
	public void applicationIsStarting() {
		assertTrue(application.isRunning());
	}
}
