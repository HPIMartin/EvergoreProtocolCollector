package dev.schoenberg.evergore.protocolParser.acceptance.browser;

import java.net.URI;
import java.net.URLEncoder;

import org.openqa.selenium.By;

import dev.schoenberg.evergore.protocolParser.acceptance.world.ServiceLog;

import static java.nio.charset.StandardCharsets.UTF_8;

public final class LoginForm {
	private static final String LOGIN_PAGE = "/login";
	private static final String ADMIN = "admin";
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
	private final ServiceLog log;
	private String lastName;

	LoginForm(MemberBrowser browser, ServiceLog log) {
		this.browser = browser;
		this.log = log;
	}

	void logInWithTheAdminKeyToOpen(String path) {
		browser.openHref(LOGIN_PAGE + "?next=" + URLEncoder.encode(path, UTF_8));
		logIn(ADMIN, log.adminKeyNamed());
	}

	public boolean wasLastUsedByTheAdmin() {
		return ADMIN.equalsIgnoreCase(lastName);
	}

	public void logIn(String name, String password) {
		lastName = name;
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
		browser.fillIn(label, text);
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
