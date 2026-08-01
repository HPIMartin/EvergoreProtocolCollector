package dev.schoenberg.evergore.protocolParser;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import dev.schoenberg.evergore.protocolParser.helper.config.Configuration;
import dev.schoenberg.evergore.protocolParser.helper.selenium.Browser;

import static java.util.Arrays.stream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Pins that the <em>configured</em> browser mode can actually drive a real browser: it starts one, loads a page and reads an element back. Selenium Manager resolves the matching
 * driver itself (into {@code ~/.cache/selenium}), so a browser binary on the {@code PATH} is the only prerequisite. Uses a {@code data:} URL, so nothing here touches the network
 * or the live game.
 * <p>
 * Skipped where no browser exists, which is deliberate rather than lenient: the production image's build stage runs {@code check} on {@code eclipse-temurin:25-jdk}, and that stage
 * must not grow a browser dependency. A skip is visible in the report; a missing browser never reads as a pass.
 */
class DockerBrowserSmokeTest {
	private static final String MARKER = "evergore";

	@Test
	void configuredBrowserModeDrivesARealBrowser() {
		assumeTrue(browserIsOnPath(), "no firefox on the PATH: browser-driven check skipped");

		Configuration config = new Configuration();
		WebDriver driver = Browser.fromString(config.browser).getDriver(config);
		try {
			driver.get("data:text/html,<html><body><h1 id='marker'>" + MARKER + "</h1></body></html>");

			assertThat(driver.findElement(By.id("marker")).getText()).isEqualTo(MARKER);
		} finally {
			driver.quit();
		}
	}

	private static boolean browserIsOnPath() {
		String path = System.getenv("PATH");
		if (path == null) {
			return false;
		}
		return stream(path.split(File.pathSeparator)).anyMatch(DockerBrowserSmokeTest::containsABrowser);
	}

	private static boolean containsABrowser(String directory) {
		return new File(directory, "firefox").canExecute() || new File(directory, "firefox-esr").canExecute();
	}
}
