package dev.schoenberg.evergore.protocolParser.acceptance.browser;

import org.openqa.selenium.By;

public final class CollectionStart {
	private static final String BUTTON = "[data-testid=start-collection]";
	private static final String ANSWERED = "document.querySelector('[data-testid=collection-state]') !== null";

	private final MemberBrowser browser;

	CollectionStart(MemberBrowser browser) {
		this.browser = browser;
	}

	public void click() {
		browser.driver().findElement(By.cssSelector(BUTTON)).click();
		browser.awaitUntil(ANSWERED);
	}
}
