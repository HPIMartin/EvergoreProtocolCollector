package dev.schoenberg.evergore.protocolParser.acceptance.browser;

import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.Select;

public final class AccountRoles {
	private static final String APPLIED = "(() => { const row = [...document.querySelectorAll('[data-testid=account-row]')]"
			+ ".find((candidate) => candidate.querySelector('[data-testid=account-name]').textContent.trim() === arguments[0]);"
			+ " const field = row === undefined ? null : row.querySelector('[data-testid=account-role]');"
			+ " return field !== null && field.tagName === 'SELECT' && !field.disabled && field.selectedOptions[0].textContent.trim() === arguments[1] })()";

	private final MemberBrowser browser;

	AccountRoles(MemberBrowser browser) {
		this.browser = browser;
	}

	public void choose(String name, String label) {
		Select field = new Select(browser.driver().findElement(By.xpath(rowOf(name) + "//select[@data-testid='account-role']")));
		field.selectByVisibleText(label);
		browser.awaitUntil(APPLIED, name, label);
	}

	private static String rowOf(String name) {
		return "//tr[@data-testid='account-row'][td[@data-testid='account-name'][normalize-space(.)='" + name + "']]";
	}
}
