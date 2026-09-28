package dev.schoenberg.evergore.protocolParser.acceptance.steps;

import java.time.LocalDateTime;

import io.cucumber.java.en.Given;

import dev.schoenberg.evergore.protocolParser.acceptance.browser.MemberBrowser;
import dev.schoenberg.evergore.protocolParser.acceptance.world.ScenarioTime;

public class ClockSteps {
	private final ScenarioTime time;
	private final MemberBrowser browser;

	public ClockSteps(ScenarioTime time, MemberBrowser browser) {
		this.time = time;
		this.browser = browser;
	}

	@Given("the clocks of the service and of the member's browser read {moment}")
	public void theClocksRead(LocalDateTime moment) {
		time.setTo(moment);
		browser.readsTheClock(time.now());
	}
}
