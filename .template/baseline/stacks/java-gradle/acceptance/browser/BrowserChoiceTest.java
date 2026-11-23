package {{BASE_PACKAGE}}.acceptance.browser;

import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.firefox.FirefoxOptions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class BrowserChoiceTest {
    private static final String A_GRID = "http://grid.example.invalid:4444";
    private static final String NO_GRID = null;

    @ParameterizedTest
    @ValueSource(strings = {"chrome", "edge"})
    void aBrowserOtherThanFirefoxWithoutTheGridIsRefusedNamingTheGridSetting(String browser) {
        BrowserChoice choice = new BrowserChoice(NO_GRID, browser);

        Throwable outcome = catchThrowable(choice::start);

        assertThat(outcome).isInstanceOf(IllegalStateException.class)
                .hasMessage("Only firefox runs without the grid; " + browser + " needs the grid and ACCEPTANCE_GRID");
    }

    @ParameterizedTest
    @CsvSource({"chrome,", "edge,", "safari,", "firefox,http://grid.example.invalid:4444"})
    void aTimeZoneIsRefusedForEverythingButFirefoxWithoutTheGrid(String browser, String grid) {
        BrowserChoice choice = new BrowserChoice(grid, browser);

        Throwable outcome = catchThrowable(() -> choice.startInTimeZone("Asia/Tokyo"));

        assertThat(outcome).isInstanceOf(IllegalStateException.class).hasMessage("Only firefox runs its own time zone, and only without the grid");
    }

    @Test
    void aBrowserTheGridDoesNotServeIsRefusedNamingIt() {
        BrowserChoice choice = new BrowserChoice(A_GRID, "safari");

        Throwable outcome = catchThrowable(choice::start);

        assertThat(outcome).isInstanceOf(IllegalStateException.class).hasMessage("The grid serves firefox, chrome and edge, not safari");
    }

    @Test
    void withoutTheGridTheServiceIsOnTheLocalHost() {
        BrowserChoice choice = new BrowserChoice(NO_GRID, "firefox");

        String host = choice.serviceHost();

        assertThat(host).isEqualTo("localhost");
    }

    @Test
    void onTheGridTheServiceIsReachedUnderItsServiceHostName() {
        BrowserChoice choice = new BrowserChoice(A_GRID, "firefox");

        String host = choice.serviceHost();

        assertThat(host).isEqualTo("acceptance-service");
    }

    @Test
    void everyLocalStartGetsADownloadFolderOfItsOwnUnderTheBuildDirectory() {
        Path first = BrowserChoice.newDownloadFolder();
        Path second = BrowserChoice.newDownloadFolder();

        assertThat(first).isNotEqualTo(second).isDirectory();
        assertThat(second).isDirectory();
        assertThat(first.getParent()).isEqualTo(OwnBuildDirectory.downloads());
        assertThat(second.getParent()).isEqualTo(OwnBuildDirectory.downloads());
    }

    @Test
    void aLocalStartsOptionsSaveEveryDownloadSilentlyIntoItsFolder() {
        Path folder = Path.of("somewhere", "start1");

        Map<String, Object> preferences = preferencesOf(BrowserChoice.firefox(folder));

        assertThat(preferences).containsEntry("browser.download.folderList", 2)
                .containsEntry("browser.download.dir", folder.toString())
                .containsEntry("browser.download.useDownloadDir", true)
                .containsEntry("browser.download.manager.addToRecentDocs", false);
    }

    @Test
    void theGridsOptionsCarryNoDownloadPreferences() {
        Map<String, Object> preferences = preferencesOf(BrowserChoice.firefox(null));

        assertThat(preferences).doesNotContainKeys("browser.download.folderList", "browser.download.dir", "browser.download.useDownloadDir");
    }

    @Test
    void theOptionsOfTheGridsFirefoxCarryNoDownloadPreferences() {
        Capabilities options = BrowserChoice.optionsFor("firefox");

        Map<String, Object> preferences = preferencesOf((FirefoxOptions) options);

        assertThat(preferences).doesNotContainKeys("browser.download.folderList", "browser.download.dir", "browser.download.useDownloadDir", "browser.download.manager.addToRecentDocs");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> preferencesOf(FirefoxOptions options) {
        Map<String, Object> firefoxOptions = (Map<String, Object>) options.asMap().get("moz:firefoxOptions");
        return (Map<String, Object>) firefoxOptions.get("prefs");
    }
}
