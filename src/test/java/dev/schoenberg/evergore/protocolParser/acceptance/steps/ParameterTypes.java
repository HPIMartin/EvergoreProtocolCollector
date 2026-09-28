package dev.schoenberg.evergore.protocolParser.acceptance.steps;

import java.time.LocalDateTime;

import io.cucumber.java.ParameterType;

import dev.schoenberg.evergore.protocolParser.acceptance.browser.LedgerName;
import dev.schoenberg.evergore.protocolParser.acceptance.browser.RosterName;
import dev.schoenberg.evergore.protocolParser.acceptance.browser.TokenChoice;

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

	@ParameterType("the admin page|the health report")
	public Surface surface(String text) {
		return Surface.of(text);
	}

	@ParameterType("bank|storage")
	public LedgerName ledger(String word) {
		return LedgerName.of(word);
	}

	@ParameterType("without the token|with a wrong token")
	public TokenChoice tokenChoice(String phrase) {
		return TokenChoice.of(phrase);
	}

	@ParameterType("in the credit colour|in the debit colour|uncoloured")
	public Tone tone(String phrase) {
		return Tone.of(phrase);
	}
}
