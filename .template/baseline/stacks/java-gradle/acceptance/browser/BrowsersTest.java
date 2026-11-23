package {{BASE_PACKAGE}}.acceptance.browser;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class BrowsersTest {
    @AfterEach
    void quitWhateverIsLeft() {
        catchThrowable(Browsers::quitAll);
    }

    @Test
    void aStartedBrowserIsHandedBackAsItIs() {
        RecordingDriver driver = new RecordingDriver();

        WebDriver handedBack = Browsers.started(driver.proxy());

        assertThat(handedBack).isSameAs(driver.proxy());
    }

    @Test
    void aQuitBrowserIsQuitOnceAndLeftOutOfQuitAll() {
        RecordingDriver driver = started();

        Browsers.quit(driver.proxy());
        Browsers.quitAll();

        assertThat(driver.quits()).isEqualTo(1);
    }

    @Test
    void quitAllQuitsEveryBrowserStillStartedOnce() {
        RecordingDriver first = started();
        RecordingDriver second = started();

        Browsers.quitAll();
        Browsers.quitAll();

        assertThat(first.quits()).isEqualTo(1);
        assertThat(second.quits()).isEqualTo(1);
    }

    @Test
    void quitAllQuitsTheOthersWhenOneQuitFailsAndRethrowsTheFirstFailureWithTheOthersSuppressed() {
        RecordingDriver first = started();
        RecordingDriver second = started();
        RecordingDriver third = started();
        first.failQuitWith("first");
        second.failQuitWith("second");

        Throwable outcome = catchThrowable(Browsers::quitAll);

        assertThat(List.of(first, second, third)).extracting(RecordingDriver::quits).containsExactly(1, 1, 1);
        assertThat(outcome).hasMessageMatching("first|second");
        assertThat(outcome.getSuppressed()).hasSize(1);
    }

    @Test
    void aBrowserWhoseQuitFailedIsNotQuitAgainByQuitAll() {
        RecordingDriver driver = started();
        driver.failQuitWith("failed");

        Throwable first = catchThrowable(() -> Browsers.quit(driver.proxy()));
        Browsers.quitAll();

        assertThat(first).hasMessage("failed");
        assertThat(driver.quits()).isEqualTo(1);
    }

    @Test
    void aSessionThatHasAlreadyEndedIsQuitWithoutFailure() {
        RecordingDriver driver = started();
        driver.failQuitAsTheSessionHasEnded();

        Throwable outcome = catchThrowable(() -> Browsers.quit(driver.proxy()));

        assertThat(outcome).isNull();
        assertThat(driver.quits()).isEqualTo(1);
    }

    @Test
    void quitAllQuitsTheOthersWhenOneQuitThrowsAnErrorAndRethrowsItWithNoBrowserLeftRegistered() {
        RecordingDriver first = started();
        RecordingDriver second = started();
        first.failQuitWithError("an error");
        second.failQuitWithError("another error");

        Throwable outcome = catchThrowable(Browsers::quitAll);
        Throwable again = catchThrowable(Browsers::quitAll);

        assertThat(List.of(first, second)).extracting(RecordingDriver::quits).containsExactly(1, 1);
        assertThat(outcome).isInstanceOf(AssertionError.class);
        assertThat(outcome.getSuppressed()).hasSize(1);
        assertThat(again).isNull();
    }

    private static RecordingDriver started() {
        RecordingDriver driver = new RecordingDriver();
        Browsers.started(driver.proxy());
        return driver;
    }
}
