package dev.schoenberg.evergore.protocolParser.dataExtraction.website;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Singleton;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.Sleeper;
import org.openqa.selenium.support.ui.WebDriverWait;

import dev.schoenberg.evergore.protocolParser.Logger;
import dev.schoenberg.evergore.protocolParser.dataExtraction.PageContents;
import dev.schoenberg.evergore.protocolParser.dataExtraction.PageSource;
import dev.schoenberg.evergore.protocolParser.helper.config.Configuration;
import dev.schoenberg.evergore.protocolParser.helper.config.CredentialsConfiguration;
import dev.schoenberg.evergore.protocolParser.helper.selenium.Driver;

import static dev.schoenberg.evergore.protocolParser.businessLogic.Constants.LAGER_EINTRAG_START;
import static dev.schoenberg.evergore.protocolParser.businessLogic.Constants.SERVER;
import static java.time.Duration.ofMillis;
import static java.time.Duration.ofMinutes;
import static java.util.Arrays.asList;
import static org.openqa.selenium.By.id;
import static org.openqa.selenium.By.xpath;
import static org.openqa.selenium.support.ui.ExpectedConditions.urlToBe;

@Singleton
public class SeleniumPageSource implements PageSource {

	private static final Duration WAIT_TIMEOUT = ofMinutes(1);
	private static final Duration WAIT_POLL_INTERVAL = ofMillis(500);

	private final Configuration config;
	private final CredentialsConfiguration credentials;
	private final Driver driver;
	private final Clock clock;
	private final Sleeper sleeper;
	private final Logger logger;

	public SeleniumPageSource(Configuration config, CredentialsConfiguration credentials, Driver driver, Clock clock, Sleeper sleeper, Logger logger) {
		this.config = config;
		this.credentials = credentials;
		this.driver = driver;
		this.clock = clock;
		this.sleeper = sleeper;
		this.logger = logger;
	}

	@Override
	public PageContents load() {
		WebDriver webDriver = driver.createWebDriver();
		try {
			loadEvergore(webDriver);

			List<String> bank = loadContent(webDriver, "guild_protocol&selection=2");
			List<String> lager = loadContent(webDriver, "town_protocol&selection=3");

			return new PageContents(lager, bank);
		} catch (RuntimeException e) {
			logger.error("Failed to scrape Evergore", e);
			throw e;
		} finally {
			quit(webDriver);
		}
	}

	private void quit(WebDriver webDriver) {
		try {
			webDriver.quit();
		} catch (RuntimeException e) {
			logger.error("Failed to quit the WebDriver", e);
		}
	}

	private List<String> loadContent(WebDriver driver, String protocol) {
		List<String> content = new ArrayList<>();
		int page = 1;
		boolean hasContent = true;
		do {
			String text = loadStoragePage(driver, page, protocol);
			List<String> lines = asList(text.split("\n"));
			hasContent = checkForContent(lines);
			if (hasContent) {
				content.addAll(lines);
			}
			page++;
		} while (hasContent);
		return content;
	}

	private boolean checkForContent(List<String> lines) {
		return lines.stream().anyMatch(line -> line.matches(LAGER_EINTRAG_START));
	}

	private String loadStoragePage(WebDriver driver, int page, String protocol) {
		String url = SERVER + "/" + config.server + "?page=" + protocol + "&pos=" + page;
		driver.navigate().to(url);
		wait(driver, url);
		return driver.findElement(By.tagName("body")).getText();
	}

	private void loadEvergore(WebDriver driver) {
		driver.navigate().to(SERVER + "/login");
		dismissCookieBanner(driver);
		tryToLogin(driver);
		wait(driver, SERVER + "/" + config.server);
	}

	private void dismissCookieBanner(WebDriver driver) {
		try {
			driver.findElement(xpath("//button[@class='fc-button fc-cta-consent fc-primary-button' and p[@class='fc-button-label' and text()='Einwilligen']]")).click();
		} catch (Exception e) {}
	}

	private void tryToLogin(WebDriver driver) {
		try {
			driver.findElement(id("nameInput")).sendKeys(credentials.username());
			driver.findElement(id("pwInput")).sendKeys(credentials.password());

			driver.findElement(xpath("//input[@type=\"submit\"]")).click();

			wait(driver, SERVER + "/" + "portal");
			driver.findElement(xpath("//button[@type=\"submit\"]")).click();
		} catch (Exception e) {
			logger.warn("Evergore login failed; the scrape continues unauthenticated and will find no protocol entries: " + e.getMessage());
		}
	}

	private void wait(WebDriver driver, String url) {
		logger.info("Waiting for: " + url);
		new WebDriverWait(driver, WAIT_TIMEOUT, WAIT_POLL_INTERVAL, clock, sleeper).until(urlToBe(url));
	}
}
