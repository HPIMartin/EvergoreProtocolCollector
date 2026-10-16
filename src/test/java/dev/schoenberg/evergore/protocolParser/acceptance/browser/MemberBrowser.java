package dev.schoenberg.evergore.protocolParser.acceptance.browser;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import dev.schoenberg.evergore.protocolParser.acceptance.service.RunningService;

import static java.nio.charset.StandardCharsets.UTF_8;

public final class MemberBrowser {
	private static final Duration HANG_GUARD = Duration.ofSeconds(30);
	private static final String SETTLED = "const content = document.querySelector('[data-testid=page-content]'); return content !== null && content.children.length > 0"
			+ " && document.querySelector('[data-testid=status-panel][data-variant=loading]') === null";
	private static final ObjectMapper JSON = new ObjectMapper();
	private static final String MEMBER = "member";
	private static final String GUILD = "guild";
	private static final String MARK = "mark";
	private static final String COLUMN = "column";

	private final RunningService service;
	private final BrowserChoice choice;
	private final TokenOpening tokenOpening;
	private final ShiftedClock shiftedClock;
	private final TimeZoneBrowser timeZoneBrowser;
	private WebDriver driver;
	private String lastReachedUrl;

	public MemberBrowser(RunningService service, BrowserChoice choice) {
		this.service = service;
		this.choice = choice;
		this.tokenOpening = new TokenOpening(this);
		this.shiftedClock = new ShiftedClock(() -> driver);
		this.timeZoneBrowser = new TimeZoneBrowser(choice);
	}

	public TokenOpening tokenOpening() {
		return tokenOpening;
	}

	public ShiftedClock clock() {
		return shiftedClock;
	}

	public TimeZoneBrowser timeZone() {
		return timeZoneBrowser;
	}

	public void openHref(String href) {
		driver().get(addressOf(href));
		awaitSettled();
		shiftedClock.assertIsShown();
		rememberTheReachedUrl();
	}

	public void reload() {
		driver().navigate().refresh();
		awaitSettled();
		rememberTheReachedUrl();
	}

	public void follow(String linkText) {
		String before = driver().getCurrentUrl();
		driver().findElement(By.linkText(linkText)).click();
		awaitTheMoveFrom(before);
	}

	public String lastReachedUrl() {
		return lastReachedUrl;
	}

	public void openExactly(String href) {
		driver().get(href);
		awaitSettled();
		shiftedClock.assertIsShown();
		rememberTheReachedUrl();
	}

	public void choose(String optionLabel) {
		driver().findElement(By.xpath("//label[normalize-space(.)='" + optionLabel + "']/input")).click();
	}

	public void clickTheColumnHeader(RosterName roster, String header) {
		clickTheColumnHeaderWithin("//*[@data-testid='" + (roster == RosterName.ACTIVE ? "active-roster" : "dormant-roster") + "']", header);
	}

	public void clickTheLedgerColumnHeader(String header) {
		String before = driver().getCurrentUrl();
		clickTheColumnHeaderWithin("//table[contains(concat(' ', normalize-space(@class), ' '), ' data-table ')]", header);
		awaitTheMoveFrom(before);
	}

	private void clickTheColumnHeaderWithin(String scopeXPath, String header) {
		driver().findElement(By.xpath(scopeXPath + "//button[.//span[@data-testid='column-label'][normalize-space(text())='" + header + "']]")).click();
	}

	public void awaitUntil(String condition, Object... arguments) {
		new WebDriverWait(driver(), HANG_GUARD).until(browser -> Boolean.TRUE.equals(javascript(browser).executeScript("return " + condition, arguments)));
	}

	public <T> T read(String scriptResource, Class<T> type) {
		String json = (String) javascript(driver()).executeScript(scriptFrom(scriptResource));
		try {
			return JSON.readValue(json, type);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public Overview overview() {
		return read("read-overview.js", Overview.class);
	}

	public MemberLedger ledger() {
		return read("read-ledger.js", MemberLedger.class);
	}

	public Navigation navigation() {
		return read("read-navigation.js", Navigation.class);
	}

	public Optional<String> markShownOnTheRowOf(String member) {
		return noteShown(Map.of(MEMBER, member), Map.of(MARK, true));
	}

	public Optional<String> markShownOnTheGuildRow() {
		return noteShown(Map.of(GUILD, true), Map.of(MARK, true));
	}

	public Optional<String> noteShownInTheCellOf(String member, int column) {
		return noteShown(Map.of(MEMBER, member), Map.of(COLUMN, column));
	}

	public Optional<String> noteShownInTheGuildRowsCell(int column) {
		return noteShown(Map.of(GUILD, true), Map.of(COLUMN, column));
	}

	public String colourOfTheToken(String token) {
		return (String) javascript(driver()).executeScript(scriptFrom("colour-of-token.js"), token);
	}

	public String colourOfTheCellOf(String member, int column) {
		return colourOfTheFigureIn(cellAt(Map.of(MEMBER, member), Map.of(COLUMN, column)));
	}

	public String colourAroundTheCellOf(String member, int column) {
		return cellAt(Map.of(MEMBER, member), Map.of(COLUMN, column)).findElement(By.xpath("..")).getCssValue("color");
	}

	public String colourOfTheStatLabelled(String label) {
		return colourOfTheFigureIn(statValueLabelled(label));
	}

	public String colourAroundTheStatLabelled(String label) {
		return statValueLabelled(label).findElement(By.xpath("..")).getCssValue("color");
	}

	public void leave() {
		if (driver == null) {
			return;
		}
		try {
			shiftedClock.forget();
			driver.get("about:blank");
			driver.manage().deleteAllCookies();
		} catch (RuntimeException | Error cleanupFailure) {
			giveTheDriverBackKeeping(cleanupFailure);
			throw cleanupFailure;
		}
		giveTheDriverBack(true);
	}

	private void giveTheDriverBackKeeping(Throwable cleanupFailure) {
		try {
			giveTheDriverBack(false);
		} catch (RuntimeException quitFailure) {
			cleanupFailure.addSuppressed(quitFailure);
		}
	}

	private void giveTheDriverBack(boolean cleaned) {
		WebDriver leaving = driver;
		driver = null;
		if (timeZoneBrowser.quitIfStartedHere(leaving)) {
			return;
		}
		if (cleaned) {
			Browsers.release(leaving);
		} else {
			Browsers.discard(leaving);
		}
	}

	WebDriver driver() {
		if (driver == null) {
			if (timeZoneBrowser.isRequested()) {
				driver = timeZoneBrowser.start();
				timeZoneBrowser.assertIsAppliedTo(driver);
			} else {
				driver = Browsers.acquire(choice);
			}
			shiftedClock.shiftIfRead();
		}
		return driver;
	}

	private String colourOfTheFigureIn(WebElement holder) {
		return (String) javascript(driver()).executeScript(scriptFrom("colour-of-figure.js"), holder);
	}

	private WebElement statValueLabelled(String label) {
		return (WebElement) javascript(driver()).executeScript(scriptFrom("find-stat.js"), label);
	}

	private WebElement cellAt(Map<String, Object> row, Map<String, Object> cell) {
		return (WebElement) javascript(driver()).executeScript(scriptFrom("find-cell.js"), row, cell);
	}

	private Optional<String> noteShown(Map<String, Object> row, Map<String, Object> cell) {
		Object found = javascript(driver()).executeScript(scriptFrom("find-note.js"), row, cell);
		if (!(found instanceof WebElement note)) {
			return Optional.empty();
		}
		javascript(driver()).executeScript("arguments[0].focus()", note);
		List<WebElement> hints = note.findElements(By.xpath("./*[not(@aria-hidden='true')]"));
		new WebDriverWait(driver(), HANG_GUARD)
				.withMessage("the note never shows itself to the member")
				.until(browser -> hints.stream().anyMatch(hint -> !hint.getText().isBlank()));
		return hints.stream().map(WebElement::getText).filter(text -> !text.isBlank()).findFirst().map(String::trim);
	}

	private void awaitTheMoveFrom(String before) {
		new WebDriverWait(driver(), HANG_GUARD).until(browser -> !before.equals(browser.getCurrentUrl()));
		awaitSettled();
		rememberTheReachedUrl();
	}

	private void rememberTheReachedUrl() {
		lastReachedUrl = driver().getCurrentUrl();
	}

	private void awaitSettled() {
		new WebDriverWait(driver(), HANG_GUARD).until(browser -> Boolean.TRUE.equals(javascript(browser).executeScript(SETTLED)));
	}

	Object execute(String script, Object... arguments) {
		return javascript(driver()).executeScript(script, arguments);
	}

	String addressOf(String href) {
		return "http://" + choice.serviceHost() + ":" + service.port() + href;
	}

	private static JavascriptExecutor javascript(WebDriver browser) {
		return (JavascriptExecutor) browser;
	}

	private static String scriptFrom(String resource) {
		try (InputStream script = MemberBrowser.class.getResourceAsStream("/acceptance/" + resource)) {
			if (script == null) {
				throw new IllegalStateException("No browser script " + resource);
			}
			return new String(script.readAllBytes(), UTF_8);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
