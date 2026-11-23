package {{BASE_PACKAGE}}.acceptance.browser;

import java.util.function.Supplier;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

import static org.assertj.core.api.Assertions.assertThat;

public class TimeZoneBrowser {
    private final BrowserChoice choice;
    private final Supplier<WebDriver> openedDriver;
    private String zone;

    TimeZoneBrowser(BrowserChoice choice, Supplier<WebDriver> openedDriver) {
        this.choice = choice;
        this.openedDriver = openedDriver;
    }

    public void runsIn(String requestedZone) {
        if (openedDriver.get() != null) {
            throw new IllegalStateException("The browser opened before it was asked to run in " + requestedZone
                    + ": give the time zone in a step that comes before the first step that opens a page or reads the browser");
        }
        zone = requestedZone;
    }

    boolean isRequested() {
        return zone != null;
    }

    WebDriver start() {
        return choice.startInTimeZone(zone);
    }

    void assertIsAppliedTo(WebDriver dedicated) {
        Object zoneOfTheBrowser = ((JavascriptExecutor) dedicated).executeScript("return Intl.DateTimeFormat().resolvedOptions().timeZone");

        assertThat(zoneOfTheBrowser).as("the time zone of the dedicated browser").isEqualTo(zone);
    }
}
