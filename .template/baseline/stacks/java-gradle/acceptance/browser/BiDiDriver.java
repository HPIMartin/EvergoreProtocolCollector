package {{BASE_PACKAGE}}.acceptance.browser;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.bidi.HasBiDi;

final class BiDiDriver {
    private BiDiDriver() {}

    static WebDriver of(WebDriver driver) {
        if (driver instanceof HasBiDi) {
            return driver;
        }
        throw new IllegalStateException("This browser driver is no HasBiDi, so it has no WebDriver BiDi connection; start the browser with the capability webSocketUrl set to true: " + driver);
    }
}
