package dev.schoenberg.evergore.protocolParser.acceptance.browser;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Type;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.bidi.BiDi;
import org.openqa.selenium.bidi.Command;
import org.openqa.selenium.bidi.HasBiDi;
import org.openqa.selenium.json.TypeToken;
import org.openqa.selenium.remote.Augmenter;
import org.openqa.selenium.support.ui.WebDriverWait;

import dev.schoenberg.evergore.protocolParser.acceptance.service.RunningService;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

public class MemberBrowser {
	private static final String TOKEN = "test-token";
	private static final Duration HANG_GUARD = Duration.ofSeconds(30);
	private static final Duration CLOCK_TOLERANCE = Duration.ofMinutes(1);
	private static final String SETTLED = "return document.querySelector('[data-testid=view-title]') !== null"
			+ " && document.querySelector('[data-testid=status-panel][data-variant=loading]') === null";
	private static final String SHIFTED_CLOCK = "() => { const shift = %d - Date.now(); const RealDate = Date;"
			+ " class ScenarioDate extends RealDate { constructor(...parts) { if (parts.length === 0) { super(RealDate.now() + shift) } else { super(...parts) } }"
			+ " static now() { return RealDate.now() + shift } } globalThis.Date = ScenarioDate }";
	private static final ObjectMapper JSON = new ObjectMapper();
	private static final String MEMBER = "member";
	private static final String GUILD = "guild";
	private static final String MARK = "mark";
	private static final String COLUMN = "column";
	private static final Type BIDI_RESULT = new TypeToken<Map<String, Object>>() {}.getType();

	private final RunningService service;
	private final BrowserChoice choice;
	private WebDriver driver;
	private Instant clock;
	private String clockScript;
	private String lastReachedUrl;

	public MemberBrowser(RunningService service, BrowserChoice choice) {
		this.service = service;
		this.choice = choice;
	}

	public void readsTheClock(Instant instant) {
		clock = instant;
		if (driver != null) {
			forgetTheShiftedClock();
			shiftTheClock();
		}
	}

	public void open(String path) {
		visit(path + tokenParameterSeparator(path) + "token=" + TOKEN);
	}

	public void openWithoutToken(String path) {
		visit(path);
	}

	private void visit(String address) {
		WebDriver browser = driver();
		browser.get("http://" + choice.serviceHost() + ":" + service.port() + address);
		awaitSettled();
		assertTheClockIsShifted();
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
		new WebDriverWait(driver(), HANG_GUARD).until(browser -> !before.equals(browser.getCurrentUrl()));
		awaitSettled();
		rememberTheReachedUrl();
	}

	public String lastReachedUrl() {
		return lastReachedUrl;
	}

	public void openExactly(String href) {
		driver().get(href);
		awaitSettled();
		assertTheClockIsShifted();
		rememberTheReachedUrl();
	}

	public void choose(String optionLabel) {
		driver().findElement(By.xpath("//label[normalize-space(.)='" + optionLabel + "']/input")).click();
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

	public void leave() {
		if (driver == null) {
			return;
		}
		forgetTheShiftedClock();
		driver.get("about:blank");
		driver.manage().deleteAllCookies();
		Browsers.release(driver);
		driver = null;
	}

	private WebDriver driver() {
		if (driver == null) {
			driver = Browsers.acquire(choice);
			if (clock != null) {
				shiftTheClock();
			}
		}
		return driver;
	}

	private void shiftTheClock() {
		Map<String, Object> added = bidi()
				.send(new Command<Map<String, Object>>("script.addPreloadScript", Map.of("functionDeclaration", SHIFTED_CLOCK.formatted(clock.toEpochMilli())), BIDI_RESULT));
		clockScript = (String) added.get("script");
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

	private void forgetTheShiftedClock() {
		if (clockScript != null) {
			bidi().send(new Command<Map<String, Object>>("script.removePreloadScript", Map.of("script", clockScript), BIDI_RESULT));
			clockScript = null;
		}
	}

	private void assertTheClockIsShifted() {
		if (clock == null) {
			return;
		}
		Map<String, Object> evaluated = bidi()
				.send(new Command<Map<String, Object>>("script.evaluate",
						Map.of("expression", "Date.now()", "target", Map.of("context", driver.getWindowHandle()), "awaitPromise", false), BIDI_RESULT));
		long browserNow = numberIn(evaluated.get("result")).orElseThrow(() -> new AssertionError("The browser answered no number for Date.now(): " + evaluated));

		assertThat(browserNow).as("the member's browser reads the scenario's clock").isCloseTo(clock.toEpochMilli(), within(CLOCK_TOLERANCE.toMillis()));
	}

	private static Optional<Long> numberIn(Object result) {
		return result instanceof Map<?, ?> value && value.get("value") instanceof Number now ? Optional.of(now.longValue()) : Optional.empty();
	}

	private BiDi bidi() {
		WebDriver augmented = driver instanceof HasBiDi ? driver : new Augmenter().augment(driver);
		return ((HasBiDi) augmented).getBiDi();
	}

	private void rememberTheReachedUrl() {
		lastReachedUrl = driver().getCurrentUrl();
	}

	private void awaitSettled() {
		new WebDriverWait(driver(), HANG_GUARD).until(browser -> Boolean.TRUE.equals(javascript(browser).executeScript(SETTLED)));
	}

	private static String tokenParameterSeparator(String path) {
		return path.indexOf('?') < 0 ? "?" : "&";
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
