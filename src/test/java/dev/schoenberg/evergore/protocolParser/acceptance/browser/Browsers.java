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

	public static void quitAll() {
		IDLE.clear();
		STARTED.forEach(WebDriver::quit);
		STARTED.clear();
	}
}
