package dev.schoenberg.evergore.protocolParser.acceptance.steps;

import java.time.LocalDateTime;

import io.cucumber.java.ParameterType;

import dev.schoenberg.evergore.protocolParser.acceptance.browser.RosterName;

import static dev.schoenberg.evergore.protocolParser.acceptance.world.GameProtocol.MINUTE;

public class ParameterTypes {
	@ParameterType("\\d{2}\\.\\d{2}\\.\\d{4} \\d{2}:\\d{2}")
	public LocalDateTime moment(String text) {
		return LocalDateTime.parse(text, MINUTE);
	}

	@ParameterType("active|dormant")
	public RosterName roster(String word) {
		return RosterName.of(word);
	}
}
