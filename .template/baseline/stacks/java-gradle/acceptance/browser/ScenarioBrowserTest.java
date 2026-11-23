package {{BASE_PACKAGE}}.acceptance.browser;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class ScenarioBrowserTest {
    private static final String TOKYO = "Asia/Tokyo";
    private static final Instant A_SHIFTED_CLOCK = Instant.parse("2031-05-17T10:00:00Z");

    private final List<RecordingDriver> started = Collections.synchronizedList(new ArrayList<>());
    private final List<RecordingDriver> startedInATimeZone = Collections.synchronizedList(new ArrayList<>());
    private String zoneTheDedicatedBrowserReports = TOKYO;
    private final Path downloadsOfTheStart = Path.of("build", "tmp", "downloads", "start1");
    private final BrowserChoice choice = new BrowserChoice(null, "firefox") {
        @Override
        WebDriver start() {
            RecordingDriver driver = new RecordingDriver();
            started.add(driver);
            return driver.proxy();
        }

        @Override
        Path downloads() {
            return downloadsOfTheStart;
        }

        @Override
        WebDriver startInTimeZone(String zone) {
            RecordingDriver driver = new RecordingDriver();
            driver.answerScriptsWith(zoneTheDedicatedBrowserReports);
            startedInATimeZone.add(driver);
            return driver.proxy();
        }
    };
    private final ScenarioBrowser browser = new ScenarioBrowser(choice);

    @AfterEach
    void quitWhateverIsLeft() {
        catchThrowable(Browsers::quitAll);
    }

    @Test
    void theFirstUseStartsTheBrowserOfTheChoice() {
        WebDriver driver = browser.driver();

        assertThat(driver).isSameAs(started.get(0).proxy());
    }

    @Test
    void everyFurtherUseInTheSameScenarioKeepsTheSameBrowser() {
        WebDriver first = browser.driver();

        WebDriver second = browser.driver();

        assertThat(second).isSameAs(first);
        assertThat(started).hasSize(1);
    }

    @Test
    void everyLeaveQuitsTheBrowserOfItsOwnScenarioOnce() {
        ScenarioBrowser second = new ScenarioBrowser(choice);
        browser.driver();
        second.driver();

        browser.leave();
        second.leave();

        assertThat(started).hasSize(2);
        assertThat(started).extracting(RecordingDriver::quits).containsExactly(1, 1);
    }

    @Test
    void aScenarioAfterAnotherStartsABrowserOfItsOwnAndNeverGetsTheEarlierOne() {
        browser.driver();
        browser.leave();
        ScenarioBrowser next = new ScenarioBrowser(choice);

        WebDriver nextDriver = next.driver();

        assertThat(started).hasSize(2);
        assertThat(nextDriver).isSameAs(started.get(1).proxy()).isNotSameAs(started.get(0).proxy());
    }

    @Test
    void leavingWithoutEverOpeningStartsAndQuitsNothing() {
        browser.leave();

        assertThat(started).isEmpty();
        assertThat(startedInATimeZone).isEmpty();
    }

    @Test
    void quitAllQuitsTheBrowserOfAScenarioThatNeverLeft() {
        browser.driver();

        Browsers.quitAll();

        assertThat(started.get(0).quits()).isEqualTo(1);
    }

    @Test
    void aSecondLeaveQuitsNothingMore() {
        browser.driver();
        browser.leave();

        browser.leave();

        assertThat(started.get(0).quits()).isEqualTo(1);
    }

    @Test
    void aFailedQuitFailsTheLeaveLoudly() {
        browser.driver();
        started.get(0).failQuitWith("the quit failed");

        Throwable outcome = catchThrowable(browser::leave);

        assertThat(outcome).hasMessage("the quit failed");
    }

    @Test
    void aBrowserThatFailedToStartIsQuitBeforeTheFailureIsRethrown() {
        zoneTheDedicatedBrowserReports = "Europe/Berlin";
        browser.timeZone().runsIn(TOKYO);

        Throwable outcome = catchThrowable(browser::driver);
        browser.leave();

        assertThat(outcome).isInstanceOf(AssertionError.class).hasMessageContaining(TOKYO).hasMessageContaining("Europe/Berlin");
        assertThat(startedInATimeZone.get(0).quits()).isEqualTo(1);
    }

    @Test
    void aFailedQuitOfABrowserThatFailedToStartIsSuppressedOnTheStartFailure() {
        ScenarioBrowser failing = new ScenarioBrowser(new BrowserChoice(null, "firefox") {
            @Override
            WebDriver startInTimeZone(String zone) {
                RecordingDriver dedicated = new RecordingDriver();
                dedicated.answerScriptsWith("Europe/Berlin");
                dedicated.failQuitWith("the quit failed too");
                return dedicated.proxy();
            }
        });
        failing.timeZone().runsIn(TOKYO);

        Throwable outcome = catchThrowable(failing::driver);

        assertThat(outcome).isInstanceOf(AssertionError.class);
        assertThat(outcome.getSuppressed()).extracting(Throwable::getMessage).containsExactly("the quit failed too");
    }

    @Test
    void aLeaveAfterTheSessionEndedByClosingTheOnlyWindowDoesNotFail() {
        browser.driver();
        started.get(0).failQuitAsTheSessionHasEnded();

        Throwable outcome = catchThrowable(browser::leave);

        assertThat(outcome).isNull();
    }

    @Test
    void aLeaveUnregistersTheBrowserSoQuitAllQuitsNothingMore() {
        browser.driver();
        browser.leave();

        Browsers.quitAll();

        assertThat(started.get(0).quits()).isEqualTo(1);
    }

    @Test
    void aBrowserThatFailedToStartIsUnregisteredSoQuitAllQuitsNothingMore() {
        zoneTheDedicatedBrowserReports = "Europe/Berlin";
        browser.timeZone().runsIn(TOKYO);
        catchThrowable(browser::driver);

        Browsers.quitAll();

        assertThat(startedInATimeZone.get(0).quits()).isEqualTo(1);
    }

    @Test
    void aQuitErrorOfABrowserThatFailedToStartIsSuppressedOnTheStartFailure() {
        ScenarioBrowser failing = new ScenarioBrowser(new BrowserChoice(null, "firefox") {
            @Override
            WebDriver startInTimeZone(String zone) {
                RecordingDriver dedicated = new RecordingDriver();
                dedicated.answerScriptsWith("Europe/Berlin");
                dedicated.failQuitWithError("the quit erred too");
                return dedicated.proxy();
            }
        });
        failing.timeZone().runsIn(TOKYO);

        Throwable outcome = catchThrowable(failing::driver);

        assertThat(outcome).isInstanceOf(AssertionError.class);
        assertThat(outcome.getSuppressed()).extracting(Throwable::getMessage).containsExactly("the quit erred too");
    }

    @Test
    void theDownloadFolderOfTheStartIsHandedOutAfterStartingTheBrowser() {
        Path folder = browser.downloads();

        assertThat(folder).isEqualTo(downloadsOfTheStart);
        assertThat(started).hasSize(1);
    }

    @Test
    void aBrowserOnTheGridHasNoDownloadFolderAndSaysWhy() {
        ScenarioBrowser onTheGrid = new ScenarioBrowser(new BrowserChoice(null, "firefox") {
            @Override
            WebDriver start() {
                return new RecordingDriver().proxy();
            }
        });

        Throwable outcome = catchThrowable(onTheGrid::downloads);

        assertThat(outcome).isInstanceOf(IllegalStateException.class).hasMessageContaining("grid");
    }

    @Test
    void aBrowserThatCouldNotBeStartedLeavesNothingToQuit() {
        ScenarioBrowser failing = new ScenarioBrowser(new BrowserChoice(null, "firefox") {
            @Override
            WebDriver start() {
                throw new IllegalStateException("no browser");
            }
        });

        Throwable outcome = catchThrowable(failing::driver);
        Throwable leaving = catchThrowable(failing::leave);

        assertThat(outcome).hasMessage("no browser");
        assertThat(leaving).isNull();
    }

    @Test
    void aDedicatedTimeZoneBrowserIsStartedInsteadOfTheChoiceAndQuitOnLeave() {
        browser.timeZone().runsIn(TOKYO);

        WebDriver driver = browser.driver();
        browser.leave();

        assertThat(driver).isSameAs(startedInATimeZone.get(0).proxy());
        assertThat(startedInATimeZone.get(0).quits()).isEqualTo(1);
        assertThat(started).isEmpty();
    }

    @Test
    void aZoneRequestedAfterTheBrowserOpenedFailsNamingTheOrder() {
        browser.driver();

        Throwable outcome = catchThrowable(() -> browser.timeZone().runsIn(TOKYO));

        assertThat(outcome).isInstanceOf(IllegalStateException.class).hasMessageContaining("opened before");
        assertThat(startedInATimeZone).isEmpty();
    }

    @Test
    void scenariosRunningAtOnceEachGetTheirOwnBrowser() throws Exception {
        int scenarios = 8;
        ExecutorService threads = Executors.newFixedThreadPool(scenarios);
        List<Callable<Object>> runs = new ArrayList<>();
        for (int each = 0; each < scenarios; each++) {
            ScenarioBrowser scenario = new ScenarioBrowser(choice);
            runs.add(() -> {
                scenario.driver();
                scenario.leave();
                return null;
            });
        }
        try {
            for (Future<Object> run : threads.invokeAll(runs)) {
                run.get();
            }
        } finally {
            threads.shutdownNow();
        }

        assertThat(started).hasSize(scenarios);
        assertThat(started).extracting(RecordingDriver::quits).containsOnly(1);
    }

    @Test
    void aClockGivenAfterTheBrowserOpenedFailsNamingTheBiDiInterfaceWhenTheDriverLacksIt() {
        browser.driver();

        Throwable outcome = catchThrowable(() -> browser.clock().reads(A_SHIFTED_CLOCK));

        assertThat(outcome).isInstanceOf(IllegalStateException.class).hasMessageContaining("HasBiDi");
    }

    @Test
    void aClockGivenBeforeTheBrowserOpenedFailsTheOpenNamingTheBiDiInterfaceAndQuitsTheBrowser() {
        browser.clock().reads(A_SHIFTED_CLOCK);

        Throwable outcome = catchThrowable(browser::driver);

        assertThat(outcome).isInstanceOf(IllegalStateException.class).hasMessageContaining("HasBiDi");
        assertThat(started.get(0).quits()).isEqualTo(1);
    }

    @Test
    void anAddressNamesTheLocalHostWithoutTheGrid() {
        String address = browser.addressOf(8080, "/page");

        assertThat(address).isEqualTo("http://localhost:8080/page");
    }

    @Test
    void anAddressNamesTheServiceHostOnTheGrid() {
        ScenarioBrowser onTheGrid = new ScenarioBrowser(new BrowserChoice("http://grid.example.invalid:4444", "firefox"));

        String address = onTheGrid.addressOf(8080, "/page");

        assertThat(address).isEqualTo("http://acceptance-service:8080/page");
    }
}
