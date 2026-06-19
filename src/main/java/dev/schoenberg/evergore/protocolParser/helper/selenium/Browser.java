package dev.schoenberg.evergore.protocolParser.helper.selenium;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.function.Function;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

import dev.schoenberg.evergore.protocolParser.helper.config.Configuration;

import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.silentThrow;
import static java.lang.System.setProperty;
import static java.nio.file.Files.createTempFile;
import static org.openqa.selenium.firefox.FirefoxDriver.SystemProperty.BROWSER_LOGFILE;

public enum Browser {
	FIREFOX("gecko", Browser::firefox, true),
	CHROME("chrome", Browser::chrome, true),
	EDGE("edge", Browser::edge, true),
	DOCKER("docker", Browser::firefoxDocker, false);

	public final String name;
	private final boolean localDriverRequired;

	private final Function<Configuration, WebDriver> driverSupplier;

	private Browser(String name, Function<Configuration, WebDriver> driverSupplier, boolean localDriverRequired) {
		this.name = name;
		this.driverSupplier = driverSupplier;
		this.localDriverRequired = localDriverRequired;
	}

	public WebDriver getDriver(Configuration config) {
		return driverSupplier.apply(config);
	}

	public static Browser fromString(String value) {
		for (Browser browser : values()) {
			if (value != null && value.toLowerCase().equals(browser.name)) {
				return browser;
			}
		}
		throw new RuntimeException("Invalid driver name passed as 'browser' property. Possible values: " + Arrays.toString(values()));
	}

	private static WebDriver chrome(Configuration config) {
		ChromeOptions options = new ChromeOptions();
		// options.setExperimentalOption("useAutomationExtension", false);
		options.addArguments("--whitelisted-ips");
		options.addArguments("--no-sandbox");
		options.addArguments("--disable-extensions");
		options.addArguments("--verbose");
		return new ChromeDriver(options);
	}

	private static WebDriver firefox(Configuration config) {
		FirefoxOptions options = new FirefoxOptions();
		setProperty(BROWSER_LOGFILE, getTempFilePath());
		return new FirefoxDriver(options);
	}

	private static WebDriver edge(Configuration config) {
		EdgeOptions options = new EdgeOptions();
		options.addArguments("headless");
		return new EdgeDriver(options);
	}

	private static WebDriver firefoxDocker(Configuration config) {
		// webBrowserDriverPath is a env variable set to "/usr/bin/geckodriver"
		// System.setProperty("webdriver.gecko.driver", webBrowserDriverPath);
		FirefoxOptions options = new FirefoxOptions();
		options.addArguments("--no-sandbox");
		options.addArguments("--disable-extensions");
		options.addArguments("--disable-gpu");
		options.addArguments("--window-size=1920,1200");
		options.addArguments("--ignore-certificate-errors");
		options.addArguments("--whitelisted-ips=''");
		options.addArguments("--disable-dev-shm-usage");
		options.addArguments("--window-size=1920,1080");
		options.addArguments("--lang=de");
		options.addArguments("--headless");
		options.addPreference("intl.accept_languages", "de");
		return new FirefoxDriver(options);
	}

	private static String getTempFilePath() {
		Path tempFile = silentThrow(() -> createTempFile("log", ".txt"));
		tempFile.toFile().deleteOnExit();
		return tempFile.toAbsolutePath().toString();
	}

	public boolean needsLocalDriver() {
		return localDriverRequired;
	}
}
