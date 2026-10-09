package dev.schoenberg.evergore.protocolParser.dataExtraction.website;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;

import dev.schoenberg.evergore.protocolParser.LoggerSpy;
import dev.schoenberg.evergore.protocolParser.dataExtraction.PageContents;
import dev.schoenberg.evergore.protocolParser.helper.config.Configuration;
import dev.schoenberg.evergore.protocolParser.helper.config.CredentialsConfiguration;
import dev.schoenberg.evergore.protocolParser.helper.selenium.Driver;

import static dev.schoenberg.evergore.protocolParser.businessLogic.Constants.SERVER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.openqa.selenium.By.id;
import static org.openqa.selenium.By.xpath;

class SeleniumPageSourceTest {
	private static final String USERNAME = "the-username";
	private static final String PASSWORD = "the-password";

	private final Configuration config = new Configuration();
	private final CredentialsConfiguration credentials = new CredentialsConfiguration(Optional.of(USERNAME), Optional.of(PASSWORD));
	private final LoggerSpy logger = new LoggerSpy();
	private final RecordingWebDriver webDriver = new RecordingWebDriver();
	private final MutableClock clock = new MutableClock();
	private final CountingSleeper sleeper = new CountingSleeper(clock);
	private SeleniumPageSource tested;

	@BeforeEach
	void setup() {
		webDriver.navigateOnClick(xpath("//input[@type=\"submit\"]"), SERVER + "/portal");
		webDriver.navigateOnClick(xpath("//button[@type=\"submit\"]"), SERVER + "/" + config.server);
		tested = new SeleniumPageSource(config, credentials, new FakeDriver(webDriver), new EvergoreSession(clock, sleeper), logger);
	}

	@Test
	void sendsTheConfiguredUsernameToTheLoginForm() {
		tested.load();

		assertThat(webDriver.keysSentTo(id("nameInput"))).containsExactly(USERNAME);
	}

	@Test
	void sendsTheConfiguredPasswordToTheLoginForm() {
		tested.load();

		assertThat(webDriver.keysSentTo(id("pwInput"))).containsExactly(PASSWORD);
	}

	@Test
	void anAbsentUsernameNeverReachesTheLoginForm() {
		tested = new SeleniumPageSource(config, new CredentialsConfiguration(Optional.empty(), Optional.of(PASSWORD)), new FakeDriver(webDriver),
				new EvergoreSession(clock, sleeper), logger);

		catchThrowable(tested::load);

		assertThat(webDriver.keysSentTo(id("nameInput"))).isEmpty();
	}

	@Test
	void anAbsentPasswordNeverReachesTheLoginForm() {
		tested = new SeleniumPageSource(config, new CredentialsConfiguration(Optional.of(USERNAME), Optional.empty()), new FakeDriver(webDriver),
				new EvergoreSession(clock, sleeper), logger);

		catchThrowable(tested::load);

		assertThat(webDriver.keysSentTo(id("pwInput"))).isEmpty();
	}

	@Test
	void namesTheLoginWhenItFailsSoTheScrapeErrorIsAttributable() {
		webDriver.stopNavigating();

		catchThrowable(tested::load);

		assertThat(logger.warnMessages()).singleElement().asString().contains("Evergore login failed");
	}

	@Test
	void neitherCredentialAppearsInTheLoginFailureWarning() {
		webDriver.stopNavigating();

		catchThrowable(tested::load);

		assertThat(logger.warnMessages()).noneMatch(message -> message.contains(USERNAME) || message.contains(PASSWORD));
	}

	@Test
	void quitsWebDriverAfterSuccessfulScrape() {
		tested.load();

		assertThat(webDriver.quitCalled()).isTrue();
	}

	@Test
	void quitsWebDriverWhenScrapeFails() {
		webDriver.failOnNavigate(new RuntimeException("scrape failed"));

		catchThrowable(() -> tested.load());

		assertThat(webDriver.quitCalled()).isTrue();
	}

	@Test
	void propagatesScrapeFailure() {
		webDriver.failOnNavigate(new RuntimeException("scrape failed"));

		assertThatThrownBy(() -> tested.load()).isInstanceOf(RuntimeException.class).hasMessage("scrape failed");
	}

	@Test
	void logsScrapeFailure() {
		webDriver.failOnNavigate(new RuntimeException("scrape failed"));

		catchThrowable(() -> tested.load());

		assertThat(logger.errorMessages()).containsExactly("Failed to scrape Evergore");
	}

	@Test
	void logsQuitFailureWithoutDiscardingContents() {
		webDriver.failOnQuit(new RuntimeException("quit failed"));

		PageContents result = tested.load();

		assertThat(result).isNotNull();
		assertThat(logger.errorMessages()).containsExactly("Failed to quit the WebDriver");
	}

	@Test
	void propagatesScrapeFailureWhenQuitAlsoFails() {
		RuntimeException scrapeFailure = new RuntimeException("scrape failed");
		webDriver.failOnNavigate(scrapeFailure);
		webDriver.failOnQuit(new RuntimeException("quit failed"));

		Throwable thrown = catchThrowable(() -> tested.load());

		assertThat(thrown).isSameAs(scrapeFailure);
	}

	@Test
	void logsBothFailuresWhenQuitAlsoFails() {
		RuntimeException scrapeFailure = new RuntimeException("scrape failed");
		RuntimeException quitFailure = new RuntimeException("quit failed");
		webDriver.failOnNavigate(scrapeFailure);
		webDriver.failOnQuit(quitFailure);

		catchThrowable(() -> tested.load());

		assertThat(logger.errorMessages()).containsExactly("Failed to scrape Evergore", "Failed to quit the WebDriver");
		assertThat(logger.errorThrowables()).containsExactly(scrapeFailure, quitFailure);
	}

	@Test
	void timesOutWithoutTouchingRealTimeWhenUrlNeverMatches() {
		webDriver.stopNavigating();

		Throwable thrown = catchThrowable(tested::load);

		assertThat(thrown).isInstanceOf(TimeoutException.class);
		assertThat(sleeper.callCount()).isGreaterThanOrEqualTo(100);
	}

	private static final class FakeDriver extends Driver {
		private final WebDriver webDriver;

		private FakeDriver(WebDriver webDriver) {
			super(null, null);
			this.webDriver = webDriver;
		}

		@Override
		public WebDriver createWebDriver() {
			return webDriver;
		}
	}
}
