package dev.schoenberg.evergore.protocolParser.acceptance.browser;

import java.net.URI;
import java.util.Objects;

import org.openqa.selenium.By;

public final class AccountResets {
	private static final String ADMIN_PAGE = "/admin";
	private static final String RESET = "account-reset";
	private static final String CONFIRM = "Zurücksetzen";
	private static final String ASKING = "document.querySelector('[data-testid=reset-question]') !== null";
	private static final String ISSUED_CHANGED = "(() => { const row = [...document.querySelectorAll('[data-testid=account-row]')]"
			+ ".find((candidate) => candidate.querySelector('[data-testid=account-name]').textContent.trim() === arguments[0]);"
			+ " const issued = row === undefined ? null : row.querySelector('[data-testid=account-issued]');"
			+ " return issued !== null && issued.textContent.trim() !== arguments[1] })()";

	private final MemberBrowser browser;

	AccountResets(MemberBrowser browser) {
		this.browser = browser;
	}

	public boolean isOpen() {
		return ADMIN_PAGE.equals(URI.create(browser.driver().getCurrentUrl()).getPath());
	}

	public void ask(String name) {
		browser.driver().findElement(By.xpath(rowOf(name) + "//button[@data-testid='" + RESET + "']")).click();
		browser.awaitUntil(ASKING);
	}

	public void answer(String buttonLabel) {
		browser.driver().findElement(By.xpath("//button[normalize-space(.)='" + buttonLabel + "']")).click();
	}

	public String reset(String name) {
		String issuedBefore = browser
				.read("read-admin.js", AdminPage.class)
				.accounts()
				.stream()
				.filter(account -> name.equals(account.name()))
				.map(account -> Objects.toString(account.issued(), ""))
				.findFirst()
				.orElse("");
		ask(name);
		answer(CONFIRM);
		browser.awaitUntil(ISSUED_CHANGED, name, issuedBefore);
		return browser.driver().findElement(By.xpath(rowOf(name) + "//*[@data-testid='account-issued']")).getText().trim();
	}

	private static String rowOf(String name) {
		return "//tr[@data-testid='account-row'][td[@data-testid='account-name'][normalize-space(.)='" + name + "']]";
	}
}
