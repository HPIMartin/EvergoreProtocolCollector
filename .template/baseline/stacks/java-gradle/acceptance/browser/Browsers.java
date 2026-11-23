package {{BASE_PACKAGE}}.acceptance.browser;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.openqa.selenium.NoSuchSessionException;
import org.openqa.selenium.WebDriver;

public final class Browsers {
    private static final Set<WebDriver> STARTED = ConcurrentHashMap.newKeySet();

    private Browsers() {}

    static WebDriver started(WebDriver driver) {
        STARTED.add(driver);
        return driver;
    }

    static void quit(WebDriver driver) {
        STARTED.remove(driver);
        try {
            driver.quit();
        } catch (NoSuchSessionException sessionAlreadyEnded) {
            return;
        }
    }

    public static void quitAll() {
        Throwable firstFailure = null;
        for (WebDriver driver : STARTED) {
            firstFailure = quitting(driver, firstFailure);
        }
        if (firstFailure instanceof Error error) {
            throw error;
        }
        if (firstFailure != null) {
            throw (RuntimeException) firstFailure;
        }
    }

    private static Throwable quitting(WebDriver driver, Throwable firstFailure) {
        try {
            quit(driver);
            return firstFailure;
        } catch (RuntimeException | Error failure) {
            if (firstFailure == null) {
                return failure;
            }
            firstFailure.addSuppressed(failure);
            return firstFailure;
        }
    }
}
