package dev.schoenberg.evergore.protocolParser.dataExtraction.website;

import java.time.Clock;
import java.time.Duration;

import jakarta.inject.Singleton;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Sleeper;
import org.openqa.selenium.support.ui.WebDriverWait;

import static dev.schoenberg.evergore.protocolParser.businessLogic.Constants.SERVER;
import static java.time.Duration.ofMillis;
import static java.time.Duration.ofMinutes;
import static org.openqa.selenium.By.id;
import static org.openqa.selenium.By.xpath;
import static org.openqa.selenium.support.ui.ExpectedConditions.urlToBe;

@Singleton
public class EvergoreSession {
	private static final By CONSENT_BANNER = xpath("//button[@class=\"fc-button fc-cta-consent fc-primary-button\" and p[@class=\"fc-button-label\" and text()=\"Einwilligen\"]]");
	private static final By NAME_INPUT = id("nameInput");
	private static final By PASSWORD_INPUT = id("pwInput");
	private static final By LOGIN_SUBMIT = xpath("//input[@type=\"submit\"]");
	private static final By PORTAL_SUBMIT = xpath("//button[@type=\"submit\"]");
	private static final Duration WAIT_TIMEOUT = ofMinutes(1);
	private static final Duration WAIT_POLL_INTERVAL = ofMillis(500);

	private final Clock clock;
	private final Sleeper sleeper;

	public EvergoreSession(Clock clock, Sleeper sleeper) {
		this.clock = clock;
		this.sleeper = sleeper;
	}

	public void signIn(WebDriver driver, String username, String password, String server) {
		driver.navigate().to(SERVER + "/login");
		dismissConsentBannerIfShown(driver);
		driver.findElement(NAME_INPUT).sendKeys(username);
		driver.findElement(PASSWORD_INPUT).sendKeys(password);
		driver.findElement(LOGIN_SUBMIT).click();
		awaitUrl(driver, SERVER + "/portal");
		driver.findElement(PORTAL_SUBMIT).click();
		awaitUrl(driver, SERVER + "/" + server);
	}

	public void openPage(WebDriver driver, String url) {
		driver.navigate().to(url);
		awaitUrl(driver, url);
	}

	private void dismissConsentBannerIfShown(WebDriver driver) {
		driver.findElements(CONSENT_BANNER).stream().findFirst().ifPresent(WebElement::click);
	}

	private void awaitUrl(WebDriver driver, String url) {
		new WebDriverWait(driver, WAIT_TIMEOUT, WAIT_POLL_INTERVAL, clock, sleeper).until(urlToBe(url));
	}
}
