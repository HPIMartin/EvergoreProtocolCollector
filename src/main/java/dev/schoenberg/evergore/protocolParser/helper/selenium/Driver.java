package dev.schoenberg.evergore.protocolParser.helper.selenium;

import static dev.schoenberg.evergore.protocolParser.helper.exceptionWrapper.ExceptionWrapper.*;
import static dev.schoenberg.evergore.protocolParser.helper.selenium.Browser.*;
import static java.nio.file.attribute.PosixFilePermission.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Set;

import org.openqa.selenium.WebDriver;

import dev.schoenberg.evergore.protocolParser.helper.config.Configuration;
import dev.schoenberg.evergore.protocolParser.helper.resourceFiles.ResourceFileLoader;

public class Driver {
	private static final String WEB_DRIVER_FOLDER = "webdrivers/";

	private final ResourceFileLoader loader;
	private final Configuration config;

	public Driver(ResourceFileLoader loader, Configuration config) {
		this.loader = loader;
		this.config = config;
	}

	public WebDriver createWebDriver() {
		String arch = System.getProperty("os.arch").contains("64") ? "64" : "32";
		String rawOs = System.getProperty("os.name").toLowerCase();
		String os = rawOs.contains("win") ? "win.exe" : rawOs.contains("mac") ? "mac" : "linux";

		Browser browser = fromString(config.browser);

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
		return browser.getDriver(config);
	}
}
