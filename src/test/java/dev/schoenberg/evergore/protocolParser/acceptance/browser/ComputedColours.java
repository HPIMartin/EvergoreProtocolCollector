package dev.schoenberg.evergore.protocolParser.acceptance.browser;

import java.util.Map;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

public class ComputedColours {
	private static final String MEMBER = "member";
	private static final String COLUMN = "column";

	private final MemberBrowser browser;

	ComputedColours(MemberBrowser browser) {
		this.browser = browser;
	}

	public String ofTheToken(String token) {
		return (String) browser.executeResource("colour-of-token.js", token);
	}

	public String ofTheCellOf(String member, int column) {
		return ofTheFigureIn(cellAt(Map.of(MEMBER, member), Map.of(COLUMN, column)));
	}

	public String aroundTheCellOf(String member, int column) {
		return cellAt(Map.of(MEMBER, member), Map.of(COLUMN, column)).findElement(By.xpath("..")).getCssValue("color");
	}

	public String ofTheStatLabelled(String label) {
		return ofTheFigureIn(statValueLabelled(label));
	}

	public String aroundTheStatLabelled(String label) {
		return statValueLabelled(label).findElement(By.xpath("..")).getCssValue("color");
	}

	private String ofTheFigureIn(WebElement holder) {
		return (String) browser.executeResource("colour-of-figure.js", holder);
	}

	private WebElement statValueLabelled(String label) {
		return (WebElement) browser.executeResource("find-stat.js", label);
	}

	private WebElement cellAt(Map<String, Object> row, Map<String, Object> cell) {
		return (WebElement) browser.executeResource("find-cell.js", row, cell);
	}
}
