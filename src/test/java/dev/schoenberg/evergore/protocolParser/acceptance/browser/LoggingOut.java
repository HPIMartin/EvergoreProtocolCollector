package dev.schoenberg.evergore.protocolParser.acceptance.browser;

import org.openqa.selenium.By;

public final class LoggingOut {
	private static final String LOGIN_PAGE = "/login";
	private static final String LOG_OUT = "Abmelden";

	private final MemberBrowser browser;

	LoggingOut(MemberBrowser browser) {
		this.browser = browser;
	}

	public void logOut() {
		browser.driver().findElement(By.xpath("//*[@data-testid='account-area']//button[normalize-space(.)='" + LOG_OUT + "']")).click();
		browser.awaitUntil("location.pathname === arguments[0]", LOGIN_PAGE);
		browser.awaitSettled();
		browser.rememberTheReachedUrl();
	}
}
