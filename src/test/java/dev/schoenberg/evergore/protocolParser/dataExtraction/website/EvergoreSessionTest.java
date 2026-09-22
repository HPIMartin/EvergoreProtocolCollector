package dev.schoenberg.evergore.protocolParser.dataExtraction.website;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;

import static dev.schoenberg.evergore.protocolParser.businessLogic.Constants.SERVER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.openqa.selenium.By.id;
import static org.openqa.selenium.By.xpath;

class EvergoreSessionTest {
	private static final String USERNAME = "the-username";
	private static final String PASSWORD = "the-password";
	private static final String SERVER_NAME = "zyrthania";
	private static final By CONSENT_BANNER = xpath("//button[@class=\"fc-button fc-cta-consent fc-primary-button\" and p[@class=\"fc-button-label\" and text()=\"Einwilligen\"]]");

	private final RecordingWebDriver webDriver = new RecordingWebDriver();
	private final MutableClock clock = new MutableClock();
	private final CountingSleeper sleeper = new CountingSleeper(clock);
	private final EvergoreSession tested = new EvergoreSession(clock, sleeper);

	@BeforeEach
	void setup() {
		webDriver.navigateOnClick(xpath("//input[@type=\"submit\"]"), SERVER + "/portal");
		webDriver.navigateOnClick(xpath("//button[@type=\"submit\"]"), SERVER + "/" + SERVER_NAME);
	}

	@Test
	void sendsTheUsernameToTheFieldTheLoginFormNamesForIt() {
		tested.signIn(webDriver, USERNAME, PASSWORD, SERVER_NAME);

		assertThat(webDriver.keysSentTo(id("nameInput"))).containsExactly(USERNAME);
	}

	@Test
	void sendsThePasswordToTheFieldTheLoginFormNamesForIt() {
		tested.signIn(webDriver, USERNAME, PASSWORD, SERVER_NAME);

		assertThat(webDriver.keysSentTo(id("pwInput"))).containsExactly(PASSWORD);
	}

	@Test
	void leavesTheBrowserOnTheGameRatherThanOnThePortalItPassesThrough() {
		tested.signIn(webDriver, USERNAME, PASSWORD, SERVER_NAME);

		assertThat(webDriver.getCurrentUrl()).isEqualTo(SERVER + "/" + SERVER_NAME);
	}

	@Test
	void dismissesTheConsentBannerBeforeItTouchesTheLoginForm() {
		tested.signIn(webDriver, USERNAME, PASSWORD, SERVER_NAME);

		assertThat(webDriver.clickOrder()).startsWith(CONSENT_BANNER);
	}

	@Test
	void signsInJustTheSameWhenNoConsentBannerIsShown() {
		webDriver.withoutElementsMatching(CONSENT_BANNER);

		tested.signIn(webDriver, USERNAME, PASSWORD, SERVER_NAME);

		assertThat(webDriver.getCurrentUrl()).isEqualTo(SERVER + "/" + SERVER_NAME);
	}

	@Test
	void waitsForTheRequestedPageRatherThanReadingWhateverTheBrowserStillShows() {
		webDriver.stopNavigating();

		Throwable thrown = catchThrowable(() -> tested.openPage(webDriver, SERVER + "/somewhere"));

		assertThat(thrown).isInstanceOf(TimeoutException.class);
	}

	@Test
	void waitsOutTheLoginWithoutTouchingRealTime() {
		webDriver.stopNavigating();

		catchThrowable(() -> tested.signIn(webDriver, USERNAME, PASSWORD, SERVER_NAME));

		assertThat(sleeper.callCount()).isGreaterThanOrEqualTo(100);
	}
}
