package dev.schoenberg.evergore.protocolParser.helper.selenium;

import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.*;
import static java.lang.System.*;
import static java.nio.file.Files.*;
import static org.openqa.selenium.firefox.FirefoxDriver.SystemProperty.*;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.function.Function;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

import dev.schoenberg.evergore.protocolParser.helper.config.Configuration;

public enum Browser {
	FIREFOX("gecko", Browser::firefox), CHROME("chrome", Browser::chrome);

	public final String name;

	private final Function<Configuration, WebDriver> driverSupplier;

	private Browser(String name, Function<Configuration, WebDriver> driverSupplier) {
		this.name = name;
		this.driverSupplier = driverSupplier;
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
		options.setExperimentalOption("useAutomationExtension", false);
		options.addArguments("--whitelisted-ips");
		options.addArguments("--no-sandbox");
		options.addArguments("--disable-extensions");
		options.addArguments("--verbose");
		return new ChromeDriver(options);
	}

	private static WebDriver firefox(Configuration config) {
		FirefoxOptions options = new FirefoxOptions();
		setProperty(DRIVER_USE_MARIONETTE, "true");
		setProperty(BROWSER_LOGFILE, getTempFilePath());
		return new FirefoxDriver(options);
	}

	private static String getTempFilePath() {
		Path tempFile = silentThrow(() -> createTempFile("log", ".txt"));
		tempFile.toFile().deleteOnExit();
		return tempFile.toAbsolutePath().toString();
	}
}
