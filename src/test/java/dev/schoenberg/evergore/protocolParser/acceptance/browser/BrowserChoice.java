package dev.schoenberg.evergore.protocolParser.acceptance.browser;

import java.io.File;
import java.net.URI;

import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.opentest4j.TestAbortedException;

import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.silentThrow;
import static java.util.Arrays.stream;

public class BrowserChoice {
	private static final String LOCAL_HOST = "localhost";
	private static final String DEVCONTAINER_ON_THE_GRID = "epc-devcontainer";
	private static final String FIREFOX = "firefox";
	private static final String CHROME = "chrome";
	private static final String EDGE = "edge";
	private static final String BIDI = "webSocketUrl";

	private final String grid = System.getProperty("acceptance.grid");
	private final String browser = System.getProperty("acceptance.browser", FIREFOX);

	public String serviceHost() {
		return grid == null ? LOCAL_HOST : DEVCONTAINER_ON_THE_GRID;
	}

	WebDriver start() {
		if (grid != null) {
			return new RemoteWebDriver(silentThrow(() -> URI.create(grid).toURL()), optionsFor(browser));
		}
		if (!FIREFOX.equals(browser)) {
			throw new IllegalStateException("Only firefox runs without the grid; " + browser + " needs grid/grid up and EPC_ACCEPTANCE_GRID");
		}
		if (!firefoxIsOnPath()) {
			throw new TestAbortedException("no firefox on the PATH: browser-driven scenario skipped");
		}
		return new FirefoxDriver(firefox());
	}

	private static Capabilities optionsFor(String browser) {
		return switch (browser) {
			case FIREFOX -> firefox();
			case CHROME -> chrome();
			case EDGE -> edge();
			default -> throw new IllegalStateException("The grid serves firefox, chrome and edge, not " + browser);
		};
	}

	private static FirefoxOptions firefox() {
		FirefoxOptions options = new FirefoxOptions();
		options.addArguments("--headless");
		options.addPreference("intl.accept_languages", "de");
		options.setCapability(BIDI, true);
		return options;
	}

	private static ChromeOptions chrome() {
		ChromeOptions options = new ChromeOptions();
		options.addArguments("--headless=new", "--lang=de");
		options.setCapability(BIDI, true);
		return options;
	}

	private static EdgeOptions edge() {
		EdgeOptions options = new EdgeOptions();
		options.addArguments("--headless=new", "--lang=de");
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
