package {{BASE_PACKAGE}}.acceptance.browser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.opentest4j.TestAbortedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class LocalBrowserCheckTest {
    @TempDir
    Path bin;

    private final SkippedScenarios skipped = new SkippedScenarios();

    @Test
    void aBrowserOnThePathPasses() throws IOException {
        executableIn("firefox");

        Throwable outcome = catchThrowable(() -> LocalBrowserCheck.require(bin.toString(), null, skipped));

        assertThat(outcome).isNull();
    }

    @Test
    void anEsrBrowserOnThePathPasses() throws IOException {
        executableIn("firefox-esr");

        Throwable outcome = catchThrowable(() -> LocalBrowserCheck.require(bin.toString(), null, skipped));

        assertThat(outcome).isNull();
    }

    @Test
    void noBrowserFailsTheScenarioNamingTheBrowserAndTheVariable() {
        Throwable outcome = catchThrowable(() -> LocalBrowserCheck.require(bin.toString(), null, skipped));

        assertThat(outcome).isInstanceOf(IllegalStateException.class).hasMessageContaining("firefox").hasMessageContaining("ACCEPTANCE_NO_BROWSER");
    }

    @Test
    void noPathAtAllFailsTheScenario() {
        Throwable outcome = catchThrowable(() -> LocalBrowserCheck.require(null, null, skipped));

        assertThat(outcome).isInstanceOf(IllegalStateException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"false", "", "TRUE", "yes", "1"})
    void aSettingOtherThanTheExactWordTrueStillFailsTheScenarioAndCountsNothing(String setting) {
        Throwable outcome = catchThrowable(() -> LocalBrowserCheck.require(bin.toString(), setting, skipped));

        assertThat(outcome).isInstanceOf(IllegalStateException.class);
        assertThat(skipped.count()).isZero();
    }

    @Test
    void noBrowserWithTheSettingSkipsTheScenarioAndCountsItOnTheGivenCounter() {
        int runCountBefore = SkippedScenarios.ofThisRun().count();

        Throwable outcome = catchThrowable(() -> LocalBrowserCheck.require(bin.toString(), "true", skipped));

        assertThat(outcome).isInstanceOf(TestAbortedException.class);
        assertThat(skipped.count()).isEqualTo(1);
        assertThat(SkippedScenarios.ofThisRun().count()).isEqualTo(runCountBefore);
    }

    @Test
    void aBrowserOnThePathRunsEvenWithTheSetting() throws IOException {
        executableIn("firefox");

        Throwable outcome = catchThrowable(() -> LocalBrowserCheck.require(bin.toString(), "true", skipped));

        assertThat(outcome).isNull();
        assertThat(skipped.count()).isZero();
    }

    private File executableIn(String name) throws IOException {
        File file = Files.createFile(bin.resolve(name)).toFile();
        file.setExecutable(true);
        return file;
    }
}
