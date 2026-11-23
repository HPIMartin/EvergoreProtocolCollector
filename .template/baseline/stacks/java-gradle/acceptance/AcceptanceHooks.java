package {{BASE_PACKAGE}}.acceptance;

import io.cucumber.java.After;
import io.cucumber.java.AfterAll;
import io.cucumber.java.BeforeAll;

import {{BASE_PACKAGE}}.acceptance.browser.Browsers;
import {{BASE_PACKAGE}}.acceptance.browser.ScenarioBrowser;
import {{BASE_PACKAGE}}.acceptance.browser.SkippedScenarios;

public class AcceptanceHooks {
    private final ScenarioBrowser browser;

    public AcceptanceHooks(ScenarioBrowser browser) {
        this.browser = browser;
    }

    @BeforeAll
    public static void sweepBrowsersEarlierRunsLeftBehind() {
        OrphanedBrowserSweep.sweep();
    }

    @After(order = 0)
    public void leaveTheBrowser() {
        browser.leave();
    }

    @AfterAll
    public static void quitTheBrowsers() {
        try {
            Browsers.quitAll();
        } finally {
            SkippedScenarios.report();
        }
    }
}
