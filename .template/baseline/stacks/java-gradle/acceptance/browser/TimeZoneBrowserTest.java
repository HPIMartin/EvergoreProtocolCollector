package {{BASE_PACKAGE}}.acceptance.browser;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class TimeZoneBrowserTest {
    private static final String TOKYO = "Asia/Tokyo";

    private final List<String> zonesAsked = new ArrayList<>();
    private final RecordingDriver dedicated = new RecordingDriver();
    private final BrowserChoice choice = new BrowserChoice(null, "firefox") {
        @Override
        WebDriver startInTimeZone(String zone) {
            zonesAsked.add(zone);
            return dedicated.proxy();
        }
    };
    private final RecordingDriver opened = new RecordingDriver();

    @Test
    void noZoneIsRequestedUntilOneIsGiven() {
        TimeZoneBrowser timeZoneBrowser = new TimeZoneBrowser(choice, () -> null);

        boolean requested = timeZoneBrowser.isRequested();

        assertThat(requested).isFalse();
    }

    @Test
    void aZoneGivenBeforeTheBrowserOpenedIsRequested() {
        TimeZoneBrowser timeZoneBrowser = new TimeZoneBrowser(choice, () -> null);

        timeZoneBrowser.runsIn(TOKYO);
        boolean requested = timeZoneBrowser.isRequested();

        assertThat(requested).isTrue();
    }

    @Test
    void aZoneGivenAfterTheBrowserOpenedFailsNamingTheOrderAndIsNotRequested() {
        TimeZoneBrowser timeZoneBrowser = new TimeZoneBrowser(choice, opened::proxy);

        Throwable outcome = catchThrowable(() -> timeZoneBrowser.runsIn(TOKYO));
        boolean requested = timeZoneBrowser.isRequested();

        assertThat(outcome).isInstanceOf(IllegalStateException.class).hasMessageContaining("opened before").hasMessageContaining(TOKYO);
        assertThat(requested).isFalse();
    }

    @Test
    void theStartAsksTheChoiceForTheRequestedZone() {
        TimeZoneBrowser timeZoneBrowser = new TimeZoneBrowser(choice, () -> null);
        timeZoneBrowser.runsIn(TOKYO);

        WebDriver started = timeZoneBrowser.start();

        assertThat(zonesAsked).containsExactly(TOKYO);
        assertThat(started).isSameAs(dedicated.proxy());
    }

    @Test
    void aBrowserReportingTheRequestedZonePasses() {
        TimeZoneBrowser timeZoneBrowser = new TimeZoneBrowser(choice, () -> null);
        timeZoneBrowser.runsIn(TOKYO);
        dedicated.answerScriptsWith(TOKYO);

        Throwable outcome = catchThrowable(() -> timeZoneBrowser.assertIsAppliedTo(dedicated.proxy()));

        assertThat(outcome).isNull();
    }

    @Test
    void aBrowserReportingAnotherZoneFailsNamingBothZones() {
        TimeZoneBrowser timeZoneBrowser = new TimeZoneBrowser(choice, () -> null);
        timeZoneBrowser.runsIn(TOKYO);
        dedicated.answerScriptsWith("Europe/Berlin");

        Throwable outcome = catchThrowable(() -> timeZoneBrowser.assertIsAppliedTo(dedicated.proxy()));

        assertThat(outcome).isInstanceOf(AssertionError.class).hasMessageContaining(TOKYO).hasMessageContaining("Europe/Berlin");
    }
}
