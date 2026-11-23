package {{BASE_PACKAGE}}.acceptance.browser;

import java.nio.file.Path;

import org.openqa.selenium.WebDriver;

public final class ScenarioBrowser {
    private WebDriver driver;
    private final BrowserChoice choice;
    private final ShiftedClock shiftedClock = new ShiftedClock(() -> driver);
    private final TimeZoneBrowser timeZoneBrowser;

    public ScenarioBrowser() {
        this(new BrowserChoice());
    }

    ScenarioBrowser(BrowserChoice choice) {
        this.choice = choice;
        this.timeZoneBrowser = new TimeZoneBrowser(choice, () -> driver);
    }

    public ShiftedClock clock() {
        return shiftedClock;
    }

    public TimeZoneBrowser timeZone() {
        return timeZoneBrowser;
    }

    public String addressOf(int port, String path) {
        return "http://" + choice.serviceHost() + ":" + port + path;
    }

    public void open(String address) {
        driver().get(address);
        shiftedClock.assertIsShown();
    }

    public Path downloads() {
        driver();
        Path folder = choice.downloads();
        if (folder == null) {
            throw new IllegalStateException("The browser of this scenario runs on a grid, whose downloads this checkout cannot read");
        }
        return folder;
    }

    public WebDriver driver() {
        if (driver == null) {
            startTheBrowser();
        }
        return driver;
    }

    public void leave() {
        if (driver == null) {
            return;
        }
        WebDriver leaving = driver;
        driver = null;
        Browsers.quit(leaving);
    }

    private void startTheBrowser() {
        driver = Browsers.started(timeZoneBrowser.isRequested() ? timeZoneBrowser.start() : choice.start());
        try {
            if (timeZoneBrowser.isRequested()) {
                timeZoneBrowser.assertIsAppliedTo(driver);
            }
            shiftedClock.shiftIfRead();
        } catch (RuntimeException | Error startFailure) {
            quitKeeping(startFailure);
            throw startFailure;
        }
    }

    private void quitKeeping(Throwable startFailure) {
        WebDriver failed = driver;
        driver = null;
        try {
            Browsers.quit(failed);
        } catch (RuntimeException | Error quitFailure) {
            startFailure.addSuppressed(quitFailure);
        }
    }
}
