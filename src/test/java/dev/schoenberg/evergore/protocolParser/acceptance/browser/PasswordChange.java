package dev.schoenberg.evergore.protocolParser.acceptance.browser;

import org.openqa.selenium.By;

public final class PasswordChange {
	private static final String PASSWORD_PAGE = "/password";
	private static final String CURRENT_PASSWORD = "Bisheriges Passwort";
	private static final String NEW_PASSWORD = "Neues Passwort";
	private static final String REPEATED_NEW_PASSWORD = "Neues Passwort wiederholen";
	private static final String CHANGE_THE_PASSWORD = "Passwort ändern";
	private static final String SAVE_THE_PASSWORD = "Passwort speichern";
	private static final String ANSWERED = "location.pathname !== arguments[0] || document.querySelector('[data-testid=status-panel]') !== null";

	private final MemberBrowser browser;

	PasswordChange(MemberBrowser browser) {
		this.browser = browser;
	}

	public void change(String current, String replacement, String repeated) {
		browser.driver().findElement(By.xpath("//*[@data-testid='account-area']//button[normalize-space(.)='" + CHANGE_THE_PASSWORD + "']")).click();
		browser.awaitUntil("location.pathname === arguments[0]", PASSWORD_PAGE);
		browser.fillIn(CURRENT_PASSWORD, current);
		browser.fillIn(NEW_PASSWORD, replacement);
		browser.fillIn(REPEATED_NEW_PASSWORD, repeated);
		browser.driver().findElement(By.xpath("//button[normalize-space(.)='" + SAVE_THE_PASSWORD + "']")).click();
		browser.awaitUntil(ANSWERED, PASSWORD_PAGE);
		browser.awaitSettled();
		browser.rememberTheReachedUrl();
	}
}
