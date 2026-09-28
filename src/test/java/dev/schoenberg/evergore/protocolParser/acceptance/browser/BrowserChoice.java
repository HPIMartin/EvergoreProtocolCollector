package dev.schoenberg.evergore.protocolParser.acceptance.browser;

import java.io.File;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.opentest4j.TestAbortedException;

import static java.util.Arrays.stream;

public class BrowserChoice {
	private static final String LOCAL_HOST = "localhost";
	private static final String FIREFOX = "firefox";
	private static final String BIDI = "webSocketUrl";

	public String serviceHost() {
		return LOCAL_HOST;
	}

	WebDriver start() {
		if (!firefoxIsOnPath()) {
			throw new TestAbortedException("no firefox on the PATH: browser-driven scenario skipped");
		}
		return new FirefoxDriver(firefox());
	}

	private static FirefoxOptions firefox() {
		FirefoxOptions options = new FirefoxOptions();
		options.addArguments("--headless");
		options.addPreference("intl.accept_languages", "de");
		options.setCapability(BIDI, true);
		return options;
	}

	private static boolean firefoxIsOnPath() {
		String path = System.getenv("PATH");
		return path != null && stream(path.split(File.pathSeparator)).anyMatch(BrowserChoice::holdsFirefox);
	}

	private static boolean holdsFirefox(String directory) {
		return new File(directory, FIREFOX).canExecute() || new File(directory, "firefox-esr").canExecute();
	}
}
