package dev.schoenberg.evergore.protocolParser.dataExtraction.website;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;

import dev.schoenberg.evergore.protocolParser.LoggerSpy;
import dev.schoenberg.evergore.protocolParser.dataExtraction.PageContents;
import dev.schoenberg.evergore.protocolParser.helper.config.Configuration;
import dev.schoenberg.evergore.protocolParser.helper.selenium.Driver;

import static dev.schoenberg.evergore.protocolParser.businessLogic.Constants.SERVER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

class SeleniumPageSourceTest {
	private final Configuration config = new Configuration();
	private final LoggerSpy logger = new LoggerSpy();
	private final RecordingWebDriver webDriver = new RecordingWebDriver();
	private SeleniumPageSource tested;

	@BeforeEach
	void setup() {
		webDriver.redirect(SERVER + "/login", SERVER + "/" + config.server);
		tested = new SeleniumPageSource(config, new FakeDriver(webDriver), logger);
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
