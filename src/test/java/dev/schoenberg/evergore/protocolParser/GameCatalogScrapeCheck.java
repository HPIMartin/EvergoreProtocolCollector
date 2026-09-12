package dev.schoenberg.evergore.protocolParser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Sleeper;
import org.openqa.selenium.support.ui.WebDriverWait;

import dev.schoenberg.evergore.protocolParser.helper.config.Configuration;
import dev.schoenberg.evergore.protocolParser.helper.selenium.Browser;

import static dev.schoenberg.evergore.protocolParser.businessLogic.Constants.SERVER;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.Duration.ofMillis;
import static java.time.Duration.ofMinutes;
import static java.util.Arrays.stream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openqa.selenium.By.id;
import static org.openqa.selenium.By.xpath;
import static org.openqa.selenium.support.ui.ExpectedConditions.urlToBe;

@EnabledIfSystemProperty(named = "gameCatalog.scrape", matches = "true", disabledReason = "on-demand: ./run-game-scrape.sh '-DgameCatalog.pages=<comma-separated page parameters>', see testing.md")
class GameCatalogScrapeCheck {
	private static final Path OUTPUT = Path.of("build/tmp/gameCatalog");
	private static final Duration WAIT_TIMEOUT = ofMinutes(1);
	private static final Duration WAIT_POLL_INTERVAL = ofMillis(500);

	@Test
	void dumpsTheGamePagesTheItemCatalogIsReadFrom() throws Exception {
		List<String> pages = requestedPages();
		assertThat(pages).as("gameCatalog.pages named no page, so the scrape would dump nothing and still pass").isNotEmpty();
		assertThat(pages).as("two page parameters that dump to one file would overwrite each other's evidence").doesNotHaveDuplicates();
		assertThat(pages.stream().map(GameCatalogScrapeCheck::dumpNameOf).toList())
				.as("two page parameters that dump to one file would overwrite each other's evidence")
				.doesNotHaveDuplicates();

		Files.createDirectories(OUTPUT);
		Configuration config = new Configuration();
		WebDriver driver = Browser.fromString(config.browser).getDriver(config);
		try {
			signIn(driver, config);
			dumpPage(driver, "01-portal");
			for (String page : pages) {
				String url = SERVER + "/" + config.server + "?page=" + page;
				driver.navigate().to(url);
				waitForUrl(driver, url);
				dumpPage(driver, dumpNameOf(page));
			}
		} finally {
			quitWithoutMaskingTheScrapeFailure(driver);
		}
	}

	private static List<String> requestedPages() {
		return stream(System.getProperty("gameCatalog.pages", "").split(",")).map(String::trim).filter(page -> !page.isEmpty()).toList();
	}

	private static String dumpNameOf(String page) {
		return "page-" + page.replaceAll("[^A-Za-z0-9]+", "-");
	}

	private void signIn(WebDriver driver, Configuration config) {
		driver.navigate().to(SERVER + "/login");
		dismissCookieBannerIfPresent(driver);
		driver.findElement(id("nameInput")).sendKeys(requiredEnvironmentValue("EVERGORE_CREDENTIALS_USERNAME"));
		driver.findElement(id("pwInput")).sendKeys(requiredEnvironmentValue("EVERGORE_CREDENTIALS_PASSWORD"));
		driver.findElement(xpath("//input[@type=\"submit\"]")).click();
		waitForUrl(driver, SERVER + "/portal");
		driver.findElement(xpath("//button[@type=\"submit\"]")).click();
		waitForUrl(driver, SERVER + "/" + config.server);
	}

	private static String requiredEnvironmentValue(String name) {
		String value = System.getenv(name);
		if (value == null || value.isBlank()) {
			throw new IllegalStateException(name + " is not set; ./run-game-scrape.sh exports it from the credentials file");
		}
		return value;
	}

	private void dismissCookieBannerIfPresent(WebDriver driver) {
		driver
				.findElements(xpath("//button[@class=\"fc-button fc-cta-consent fc-primary-button\" and p[@class=\"fc-button-label\" and text()=\"Einwilligen\"]]"))
				.stream()
				.findFirst()
				.ifPresent(WebElement::click);
	}

	private void waitForUrl(WebDriver driver, String url) {
		new WebDriverWait(driver, WAIT_TIMEOUT, WAIT_POLL_INTERVAL, Clock.systemDefaultZone(), Sleeper.SYSTEM_SLEEPER).until(urlToBe(url));
	}

	private void quitWithoutMaskingTheScrapeFailure(WebDriver driver) {
		try {
			driver.quit();
		} catch (RuntimeException thrownWhileTheRealFailureIsStillInFlight) {}
	}

	private void dumpPage(WebDriver driver, String name) throws Exception {
		Files.writeString(OUTPUT.resolve(name + ".txt"), driver.getCurrentUrl() + "\n\n" + driver.findElement(By.tagName("body")).getText(), UTF_8);
		Files.writeString(OUTPUT.resolve(name + ".html"), driver.getPageSource(), UTF_8);
		Files.writeString(OUTPUT.resolve(name + ".tsv"), linkTable(driver), UTF_8);
		assertThat(OUTPUT.resolve(name + ".txt")).as("the dump of %s is empty, so the session carried no page", name).isNotEmptyFile();
	}

	private String linkTable(WebDriver driver) {
		StringBuilder table = new StringBuilder();
		for (WebElement link : driver.findElements(By.tagName("a"))) {
			table.append(link.getText().replace("\n", " ")).append("\t").append(link.getAttribute("href")).append("\n");
		}
		return table.toString();
	}
}
