package dev.schoenberg.evergore.protocolParser.acceptance.browser;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

import org.openqa.selenium.WebDriver;

public final class Browsers {
	private static final Queue<WebDriver> IDLE = new ConcurrentLinkedQueue<>();
	private static final List<WebDriver> STARTED = new CopyOnWriteArrayList<>();

	private Browsers() {}

	static WebDriver acquire(BrowserChoice choice) {
		WebDriver idle = IDLE.poll();
		if (idle != null) {
			return idle;
		}
		WebDriver started = choice.start();
		STARTED.add(started);
		return started;
	}

	static void release(WebDriver driver) {
		IDLE.add(driver);
	}

	static void discard(WebDriver driver) {
		driver.quit();
		STARTED.remove(driver);
	}

	public static void quitAll() {
		IDLE.clear();
		RuntimeException firstFailure = null;
		try {
			for (WebDriver driver : STARTED) {
				firstFailure = quitting(driver, firstFailure);
			}
		} finally {
			STARTED.clear();
		}
		if (firstFailure != null) {
			throw firstFailure;
		}
	}

	private static RuntimeException quitting(WebDriver driver, RuntimeException firstFailure) {
		try {
			driver.quit();
			return firstFailure;
		} catch (RuntimeException failure) {
			if (firstFailure == null) {
				return failure;
			}
			firstFailure.addSuppressed(failure);
			return firstFailure;
		}
	}
}
