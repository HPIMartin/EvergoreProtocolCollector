package dev.schoenberg.evergore.protocolParser.acceptance.browser;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URLEncoder;
import java.time.Duration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import dev.schoenberg.evergore.protocolParser.acceptance.service.RunningService;
import dev.schoenberg.evergore.protocolParser.acceptance.world.ServiceLog;

import static java.nio.charset.StandardCharsets.UTF_8;

public final class MemberBrowser {
	static final Duration HANG_GUARD = Duration.ofSeconds(30);
	private static final String SETTLED = "const content = document.querySelector('[data-testid=page-content]');"
			+ " if (document.querySelector('main.login-page [data-testid=view-title]') !== null) { return true }"
			+ " return content !== null && content.children.length > 0 && document.querySelector('[data-testid=status-panel][data-variant=loading]') === null";
	private static final ObjectMapper JSON = new ObjectMapper();

	private final RunningService service;
	private final BrowserChoice choice;
	private final DashboardOpening opening;
	private final LoginForm loginForm;
	private final PasswordChange passwordChange;
	private final ShiftedClock shiftedClock;
	private final TimeZoneBrowser timeZoneBrowser;
	private final ComputedColours computedColours;
	private final ShownNotes shownNotes;
	private WebDriver driver;
	private String lastReachedUrl;

	public MemberBrowser(RunningService service, BrowserChoice choice, ServiceLog log) {
		this.service = service;
		this.choice = choice;
		this.opening = new DashboardOpening(this);
		this.loginForm = new LoginForm(this, log);
		this.passwordChange = new PasswordChange(this);
		this.shiftedClock = new ShiftedClock(() -> driver);
		this.timeZoneBrowser = new TimeZoneBrowser(choice);
		this.computedColours = new ComputedColours(this);
		this.shownNotes = new ShownNotes(this);
	}

	public DashboardOpening opening() {
		return opening;
	}

	public LoginForm loginForm() {
		return loginForm;
	}

	public ShiftedClock clock() {
		return shiftedClock;
	}

	public TimeZoneBrowser timeZone() {
		return timeZoneBrowser;
	}

	public ComputedColours colours() {
		return computedColours;
	}

	public ShownNotes notes() {
		return shownNotes;
	}

	public void openHref(String href) {
		driver().get(addressOf(href));
		awaitSettled();
		shiftedClock.assertIsShown();
		rememberTheReachedUrl();
	}

	boolean hasALogin() {
		return driver().getCurrentUrl().startsWith("http") && driver().manage().getCookieNamed("epc_session") != null;
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

	public void followALinkFromAnotherSite(String href) {
		String page = "<a id=\"across\" href=\"" + addressOf(href) + "\">to the dashboard</a>";
		String anotherSite = "data:text/html;charset=utf-8," + URLEncoder.encode(page, UTF_8).replace("+", "%20");
		driver().get(anotherSite);
		String before = driver().getCurrentUrl();
		driver().findElement(By.id("across")).click();
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
		String json = (String) executeResource(scriptResource);
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

	public Frame frame() {
		return read("read-frame.js", Frame.class);
	}

	public PasswordChange passwordChange() {
		return passwordChange;
	}

	void fillIn(String label, String text) {
		WebElement field = driver().findElement(By.xpath("//label[normalize-space(.)='" + label + "']/input"));
		field.clear();
		if (!text.isEmpty()) {
			field.sendKeys(text);
		}
	}

	public void leave() {
		if (driver == null) {
			return;
		}
		try {
			shiftedClock.forget();
			forgetTheLogin();
			driver.get("about:blank");
		} catch (RuntimeException | Error cleanupFailure) {
			giveTheDriverBackKeeping(cleanupFailure);
			throw cleanupFailure;
		}
		giveTheDriverBack(true);
	}

	private void forgetTheLogin() {
		if (driver.getCurrentUrl().startsWith("about:")) {
			return;
		}
		if (!driver.getCurrentUrl().startsWith(addressOf("/"))) {
			driver.get(addressOf("/health"));
		}
		driver.manage().deleteAllCookies();
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

	private void awaitTheMoveFrom(String before) {
		new WebDriverWait(driver(), HANG_GUARD).until(browser -> !before.equals(browser.getCurrentUrl()));
		awaitSettled();
		rememberTheReachedUrl();
	}

	void rememberTheReachedUrl() {
		lastReachedUrl = driver().getCurrentUrl();
	}

	void awaitSettled() {
		new WebDriverWait(driver(), HANG_GUARD).until(browser -> Boolean.TRUE.equals(javascript(browser).executeScript(SETTLED)));
	}

	Object execute(String script, Object... arguments) {
		return javascript(driver()).executeScript(script, arguments);
	}

	Object executeResource(String resource, Object... arguments) {
		return execute(scriptFrom(resource), arguments);
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
