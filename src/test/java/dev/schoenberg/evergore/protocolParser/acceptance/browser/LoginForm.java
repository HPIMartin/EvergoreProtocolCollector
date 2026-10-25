package dev.schoenberg.evergore.protocolParser.acceptance.browser;

import java.net.URI;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

public final class LoginForm {
	private static final String LOGIN_PAGE = "/login";
	private static final String NAME = "Name";
	private static final String PASSWORD = "Passwort";
	private static final String NEW_PASSWORD = "Neues Passwort";
	private static final String REPEATED_NEW_PASSWORD = "Neues Passwort wiederholen";
	private static final String LOG_IN = "Anmelden";
	private static final String SAVE_THE_PASSWORD = "Passwort speichern";
	private static final int SERVED = 200;
	private static final String TITLE = "(document.querySelector('[data-testid=view-title]')?.textContent ?? null)";
	private static final String MESSAGE = "(document.querySelector('[data-testid=status-panel]')?.textContent ?? null)";
	private static final String ANSWERED = "location.href !== arguments[0] || " + MESSAGE + " !== arguments[2] || (window.epcLoginAnswer != null && (window.epcLoginAnswer !== "
			+ SERVED + " || " + TITLE + " !== arguments[1]))";

	private final MemberBrowser browser;

	LoginForm(MemberBrowser browser) {
		this.browser = browser;
	}

	public void logIn(String name, String password) {
		openTheLoginPageUnlessItIsOpen();
		fillIn(NAME, name);
		fillIn(PASSWORD, password);
		submit(LOG_IN);
	}

	public void chooseTheNewPassword(String newPassword, String repeated) {
		fillIn(NEW_PASSWORD, newPassword);
		fillIn(REPEATED_NEW_PASSWORD, repeated);
		submit(SAVE_THE_PASSWORD);
	}

	private void openTheLoginPageUnlessItIsOpen() {
		if (!LOGIN_PAGE.equals(URI.create(browser.driver().getCurrentUrl()).getPath())) {
			browser.openHref(LOGIN_PAGE);
		}
	}

	private void fillIn(String label, String text) {
		WebElement field = browser.driver().findElement(By.xpath("//label[normalize-space(.)='" + label + "']/input"));
		field.clear();
		if (!text.isEmpty()) {
			field.sendKeys(text);
		}
	}

	private void submit(String buttonLabel) {
		String urlBefore = browser.driver().getCurrentUrl();
		Object titleBefore = browser.execute("return " + TITLE);
		Object messageBefore = browser.execute("return " + MESSAGE);
		browser.executeResource("watch-login-answer.js");
		browser.driver().findElement(By.xpath("//button[normalize-space(.)='" + buttonLabel + "']")).click();
		browser.awaitUntil(ANSWERED, urlBefore, titleBefore, messageBefore);
		browser.awaitSettled();
		browser.rememberTheReachedUrl();
	}
}
