package {{BASE_PACKAGE}}.acceptance.browser;

import java.io.File;

import static java.util.Arrays.stream;

final class LocalBrowserCheck {
    static final String NO_BROWSER_VARIABLE = "ACCEPTANCE_NO_BROWSER";
    static final String NO_BROWSER_PROPERTY = "acceptance.noBrowser";
    private static final String FIREFOX = "firefox";
    private static final String FIREFOX_ESR = "firefox-esr";

    private LocalBrowserCheck() {}

    static void require() {
        require(System.getenv("PATH"), System.getProperty(NO_BROWSER_PROPERTY), SkippedScenarios.ofThisRun());
    }

    static void require(String path, String noBrowserSetting, SkippedScenarios skipped) {
        if (firefoxIsOn(path)) {
            return;
        }
        String reason = "no " + FIREFOX + " on the PATH";
        if ("true".equals(noBrowserSetting)) {
            throw skipped.skip(reason + ": browser-driven scenario skipped because " + NO_BROWSER_VARIABLE + " is true");
        }
        throw new IllegalStateException(reason + ", so this browser-driven scenario fails. Install " + FIREFOX
                + ", or set " + NO_BROWSER_VARIABLE + "=true for a run that has no browser by design, which then skips these scenarios and counts them");
    }

    private static boolean firefoxIsOn(String path) {
        return path != null && stream(path.split(File.pathSeparator)).anyMatch(LocalBrowserCheck::holdsFirefox);
    }

    private static boolean holdsFirefox(String directory) {
        return new File(directory, FIREFOX).canExecute() || new File(directory, FIREFOX_ESR).canExecute();
    }
}
