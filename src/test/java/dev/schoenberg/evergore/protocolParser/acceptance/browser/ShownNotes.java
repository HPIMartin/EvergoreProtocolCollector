package dev.schoenberg.evergore.protocolParser.acceptance.browser;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

public class ShownNotes {
	private static final String MEMBER = "member";
	private static final String GUILD = "guild";
	private static final String MARK = "mark";
	private static final String COLUMN = "column";

	private final MemberBrowser browser;

	ShownNotes(MemberBrowser browser) {
		this.browser = browser;
	}

	public Optional<String> markOnTheRowOf(String member) {
		return noteShown(Map.of(MEMBER, member), Map.of(MARK, true));
	}

	public Optional<String> markOnTheGuildRow() {
		return noteShown(Map.of(GUILD, true), Map.of(MARK, true));
	}

	public Optional<String> inTheCellOf(String member, int column) {
		return noteShown(Map.of(MEMBER, member), Map.of(COLUMN, column));
	}

	public Optional<String> inTheGuildRowsCell(int column) {
		return noteShown(Map.of(GUILD, true), Map.of(COLUMN, column));
	}

	private Optional<String> noteShown(Map<String, Object> row, Map<String, Object> cell) {
		Object found = browser.executeResource("find-note.js", row, cell);
		if (!(found instanceof WebElement note)) {
			return Optional.empty();
		}
		browser.execute("arguments[0].focus()", note);
		List<WebElement> hints = note.findElements(By.xpath("./*[not(@aria-hidden='true')]"));
		new WebDriverWait(browser.driver(), MemberBrowser.HANG_GUARD)
				.withMessage("the note never shows itself to the member")
				.until(driver -> hints.stream().anyMatch(hint -> !hint.getText().isBlank()));
		return hints.stream().map(WebElement::getText).filter(text -> !text.isBlank()).findFirst().map(String::trim);
	}
}
