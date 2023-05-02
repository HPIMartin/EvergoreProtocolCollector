package dev.schoenberg.evergore.protocolParser.dataExtraction.website;

import static dev.schoenberg.evergore.protocolParser.businessLogic.Constants.*;
import static java.nio.file.Files.*;
import static java.time.Duration.*;
import static java.util.Arrays.*;
import static org.openqa.selenium.By.*;
import static org.openqa.selenium.support.ui.ExpectedConditions.*;

import java.util.*;
import java.util.ArrayList;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;

import dev.schoenberg.evergore.protocolParser.dataExtraction.*;
import dev.schoenberg.evergore.protocolParser.helper.config.*;
import dev.schoenberg.evergore.protocolParser.helper.selenium.*;
import jakarta.inject.*;

@Singleton
public class PageContentExtractor {

	private final Configuration config;
	private final FileLoader fileLoader;

	public PageContentExtractor(Configuration config, FileLoader fileLoader) {
		this.config = config;
		this.fileLoader = fileLoader;
	}

	public PageContents load() {
		WebDriver driver = new Driver(fileLoader, config).createWebDriver();
		loadEvergore(driver);

		List<String> bank = loadContent(driver, "guild_protocol&selection=2"); // Bank
		List<String> lager = loadContent(driver, "town_protocol&selection=3"); // Lager

		PageContents result = new PageContents(lager, bank);

		try {
			driver.close();
		} catch (Exception e) {
		}

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
		for (String line : lines)
			if (line.matches(LAGER_EINTRAG_START)) {
				return true;
			}
		return false;
	}

	private String loadStoragePage(WebDriver driver, int page, String protocol) {
		String url = SERVER + "/" + config.server + "?page=" + protocol + "&pos=" + page;
		driver.navigate().to(url);
		wait(driver, url);
		return driver.findElement(By.tagName("body")).getText();
	}

	private void loadEvergore(WebDriver driver) {
		driver.navigate().to(SERVER + "/login");
		tryToLogin(driver);
		wait(driver, SERVER + "/" + config.server);
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
			} catch (Exception e) {
			}
		}
	}

	private void wait(WebDriver driver, String url) {
		System.out.println("Waiting for: " + url);
		new WebDriverWait(driver, ofMinutes(1)).until(urlToBe(url));
	}
}
