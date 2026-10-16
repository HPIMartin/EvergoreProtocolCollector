package dev.schoenberg.evergore.protocolParser.acceptance.browser;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

import static org.assertj.core.api.Assertions.assertThat;

public class TimeZoneBrowser {
	private final BrowserChoice choice;
	private String zone;
	private boolean started;

	TimeZoneBrowser(BrowserChoice choice) {
		this.choice = choice;
	}

	public void runsIn(String requestedZone) {
		zone = requestedZone;
	}

	boolean isRequested() {
		return zone != null;
	}

	WebDriver start() {
		WebDriver dedicated = choice.startInTimeZone(zone);
		started = true;
		return dedicated;
	}

	void assertIsAppliedTo(WebDriver dedicated) {
		Object zoneOfTheBrowser = ((JavascriptExecutor) dedicated).executeScript("return Intl.DateTimeFormat().resolvedOptions().timeZone");

		assertThat(zoneOfTheBrowser).as("the time zone of the member's dedicated browser").isEqualTo(zone);
	}

	boolean quitIfStartedHere(WebDriver leaving) {
		if (!started) {
			return false;
		}
		started = false;
		leaving.quit();
		return true;
	}
}
