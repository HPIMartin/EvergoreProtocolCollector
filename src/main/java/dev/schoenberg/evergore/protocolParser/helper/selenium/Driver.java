package dev.schoenberg.evergore.protocolParser.helper.selenium;

import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.*;
import static dev.schoenberg.evergore.protocolParser.helper.selenium.Browser.*;
import static java.nio.file.attribute.PosixFilePermission.*;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.*;

import org.openqa.selenium.*;

import dev.schoenberg.evergore.protocolParser.helper.config.*;

public class Driver {
	private static final String WEB_DRIVER_FOLDER = "webdrivers/";

	private final FileLoader loader;
	private final Configuration config;

	public Driver(FileLoader loader, Configuration config) {
		this.loader = loader;
		this.config = config;
	}

	public WebDriver createWebDriver() {
		Browser browser = fromString(config.browser);
		if (browser.needsLocalDriver()) {
			prepareLocalDriver(browser);
		}
		return browser.getDriver(config);
	}

	private void prepareLocalDriver(Browser browser) {
		String arch = System.getProperty("os.arch").contains("64") ? "64" : "32";
		String rawOs = System.getProperty("os.name").toLowerCase();
		String os = rawOs.contains("win") ? "win.exe" : rawOs.contains("mac") ? "mac" : "linux";

		String driverFileName = browser.name + "-" + arch + "-" + os;

		File driver = loader.fetchFile(WEB_DRIVER_FOLDER + driverFileName);
		if (!"win.exe".equals(os)) {
			Set<PosixFilePermission> perms = new HashSet<>();
			perms.add(OWNER_READ);
			perms.add(OWNER_WRITE);
			perms.add(OWNER_EXECUTE);
			silentThrow(() -> Files.setPosixFilePermissions(driver.toPath(), perms));
		}

		System.setProperty("webdriver." + browser.name + ".driver", driver.getAbsolutePath());
	}
}
