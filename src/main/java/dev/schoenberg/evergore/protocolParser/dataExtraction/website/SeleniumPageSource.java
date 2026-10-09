package dev.schoenberg.evergore.protocolParser.dataExtraction.website;

import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Singleton;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import dev.schoenberg.evergore.protocolParser.Logger;
import dev.schoenberg.evergore.protocolParser.dataExtraction.PageContents;
import dev.schoenberg.evergore.protocolParser.dataExtraction.PageSource;
import dev.schoenberg.evergore.protocolParser.helper.config.Configuration;
import dev.schoenberg.evergore.protocolParser.helper.config.CredentialsConfiguration;
import dev.schoenberg.evergore.protocolParser.helper.selenium.Driver;

import static dev.schoenberg.evergore.protocolParser.businessLogic.Constants.LAGER_EINTRAG_START;
import static dev.schoenberg.evergore.protocolParser.businessLogic.Constants.SERVER;
import static java.util.Arrays.asList;

@Singleton
public class SeleniumPageSource implements PageSource {

	private final Configuration config;
	private final CredentialsConfiguration credentials;
	private final Driver driver;
	private final EvergoreSession session;
	private final Logger logger;

	public SeleniumPageSource(Configuration config, CredentialsConfiguration credentials, Driver driver, EvergoreSession session, Logger logger) {
		this.config = config;
		this.credentials = credentials;
		this.driver = driver;
		this.session = session;
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
		logger.info("Waiting for: " + url);
		session.openPage(driver, url);
		return driver.findElement(By.tagName("body")).getText();
	}

	private void loadEvergore(WebDriver driver) {
		try {
			session.signIn(driver, credentials.username().orElseThrow(), credentials.password().orElseThrow(), config.server);
		} catch (RuntimeException e) {
			logger.warn("Evergore login failed, so the scrape reaches no protocol entries: " + e.getMessage());
			throw e;
		}
	}
}
