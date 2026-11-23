package {{BASE_PACKAGE}}.acceptance.browser;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.GeckoDriverService;
import org.openqa.selenium.remote.RemoteWebDriver;

public class BrowserChoice {
    private static final String LOCAL_HOST = "localhost";
    private static final String SERVICE_HOST_ON_THE_GRID = "acceptance-service";
    private static final String BROWSER_LOCALE = "en";
    private static final String FIREFOX = "firefox";
    private static final String CHROME = "chrome";
    private static final String EDGE = "edge";
    private static final String BIDI = "webSocketUrl";

    private final String grid;
    private final String browser;
    private Path downloads;

    public BrowserChoice() {
        this(System.getProperty("acceptance.grid"), System.getProperty("acceptance.browser", FIREFOX));
    }

    BrowserChoice(String grid, String browser) {
        this.grid = grid;
        this.browser = browser;
    }

    public String serviceHost() {
        return grid == null ? LOCAL_HOST : SERVICE_HOST_ON_THE_GRID;
    }

    WebDriver start() {
        if (grid != null) {
            return new RemoteWebDriver(gridUrl(), optionsFor(browser));
        }
        if (!FIREFOX.equals(browser)) {
            throw new IllegalStateException("Only " + FIREFOX + " runs without the grid; " + browser + " needs the grid and ACCEPTANCE_GRID");
        }
        return startFirefox(Map.of());
    }

    WebDriver startInTimeZone(String zone) {
        if (grid != null || !FIREFOX.equals(browser)) {
            throw new IllegalStateException("Only " + FIREFOX + " runs its own time zone, and only without the grid");
        }
        return startFirefox(Map.of("TZ", zone));
    }

    private WebDriver startFirefox(Map<String, String> environment) {
        LocalBrowserCheck.require();
        downloads = newDownloadFolder();
        return new FirefoxDriver(geckoDriver(environment), firefox(downloads));
    }

    Path downloads() {
        return downloads;
    }

    static Path newDownloadFolder() {
        try {
            Files.createDirectories(OwnBuildDirectory.downloads());
            return Files.createTempDirectory(OwnBuildDirectory.downloads(), "start");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private URL gridUrl() {
        try {
            return URI.create(grid).toURL();
        } catch (MalformedURLException e) {
            throw new IllegalStateException("acceptance.grid is no URL: " + grid, e);
        }
    }

    private static GeckoDriverService geckoDriver(Map<String, String> environment) {
        try {
            Files.createDirectories(OwnBuildDirectory.browserProfiles());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return new GeckoDriverService.Builder()
                .usingDriverExecutable(GeckoDriverExecutable.ofThisJvm())
                .withProfileRoot(OwnBuildDirectory.browserProfiles().toFile())
                .withEnvironment(environment)
                .build();
    }

    static Capabilities optionsFor(String browser) {
        return switch (browser) {
            case FIREFOX -> firefox(null);
            case CHROME -> chrome();
            case EDGE -> edge();
            default -> throw new IllegalStateException("The grid serves firefox, chrome and edge, not " + browser);
        };
    }

    static FirefoxOptions firefox(Path downloadFolder) {
        FirefoxOptions options = new FirefoxOptions();
        if (downloadFolder != null) {
            options.addPreference("browser.download.folderList", 2);
            options.addPreference("browser.download.dir", downloadFolder.toString());
            options.addPreference("browser.download.useDownloadDir", true);
            options.addPreference("browser.download.manager.addToRecentDocs", false);
        }
        options.addArguments("--headless");
        options.addPreference("intl.accept_languages", BROWSER_LOCALE);
        options.setCapability(BIDI, true);
        return options;
    }

    private static ChromeOptions chrome() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new", "--lang=" + BROWSER_LOCALE);
        options.setCapability(BIDI, true);
        return options;
    }

    private static EdgeOptions edge() {
        EdgeOptions options = new EdgeOptions();
        options.addArguments("--headless=new", "--lang=" + BROWSER_LOCALE);
        options.setCapability(BIDI, true);
        return options;
    }
}
