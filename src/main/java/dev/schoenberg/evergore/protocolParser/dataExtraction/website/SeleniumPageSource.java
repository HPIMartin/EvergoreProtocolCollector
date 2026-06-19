package dev.schoenberg.evergore.protocolParser.dataExtraction.website;

import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Singleton;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

import dev.schoenberg.evergore.protocolParser.dataExtraction.PageContents;
import dev.schoenberg.evergore.protocolParser.dataExtraction.PageSource;
import dev.schoenberg.evergore.protocolParser.helper.config.Configuration;
import dev.schoenberg.evergore.protocolParser.helper.selenium.Driver;

import static dev.schoenberg.evergore.protocolParser.businessLogic.Constants.LAGER_EINTRAG_START;
import static dev.schoenberg.evergore.protocolParser.businessLogic.Constants.SERVER;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.readAllLines;
import static java.time.Duration.ofMinutes;
import static java.util.Arrays.asList;
import static org.openqa.selenium.By.id;
import static org.openqa.selenium.By.xpath;
import static org.openqa.selenium.support.ui.ExpectedConditions.urlToBe;

@Singleton
public class SeleniumPageSource implements PageSource {

	private final Configuration config;
	private final Driver driver;

	public SeleniumPageSource(Configuration config, Driver driver) {
		this.config = config;
		this.driver = driver;
	}

	@Override
	public PageContents load() {
		WebDriver webDriver = driver.createWebDriver();
		loadEvergore(webDriver);

		List<String> bank = loadContent(webDriver, "guild_protocol&selection=2");
		List<String> lager = loadContent(webDriver, "town_protocol&selection=3");

		PageContents result = new PageContents(lager, bank);

		try {
			webDriver.close();
		} catch (Exception e) {}

		return result;
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
		} catch (Exception e) {
			// NOOP
		}
	}

	private void tryToLogin(WebDriver driver) {
		if (exists(config.credentials)) {
			try {
				List<String> content = readAllLines(config.credentials);
				String username = content.get(0);
				String password = content.get(1);

				driver.findElement(id("nameInput")).sendKeys(username);
				driver.findElement(id("pwInput")).sendKeys(password);

				driver.findElement(xpath("//input[@type=\"submit\"]")).click();

				wait(driver, SERVER + "/" + "portal");
				driver.findElement(xpath("//button[@type=\"submit\"]")).click();
			} catch (Exception e) {}
		}
	}

	private void wait(WebDriver driver, String url) {
		System.out.println("Waiting for: " + url);
		new WebDriverWait(driver, ofMinutes(1)).until(urlToBe(url));
	}
}
