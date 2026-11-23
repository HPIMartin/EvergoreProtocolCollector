package {{BASE_PACKAGE}}.acceptance.browser;

import java.time.Instant;
import java.util.Locale;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

@ResourceLock(Resources.LOCALE)
class ShiftedClockTest {
    private static final Instant NEW_YEAR_2026 = Instant.parse("2026-01-01T00:00:00Z");

    private final Locale defaultLocale = Locale.getDefault();
    private final Locale displayLocale = Locale.getDefault(Locale.Category.DISPLAY);
    private final Locale formatLocale = Locale.getDefault(Locale.Category.FORMAT);

    static Stream<Locale> localesWritingThaiDigits() {
        return Stream.of(Locale.forLanguageTag("th-TH-u-nu-thai"), Locale.of("th", "TH", "TH"));
    }

    @AfterEach
    void restoreTheDefaultLocale() {
        Locale.setDefault(defaultLocale);
        Locale.setDefault(Locale.Category.DISPLAY, displayLocale);
        Locale.setDefault(Locale.Category.FORMAT, formatLocale);
    }

    @ParameterizedTest
    @MethodSource("localesWritingThaiDigits")
    void theScriptCarriesTheInstantInAsciiDigitsUnderADefaultLocaleWritingOtherDigits(Locale localeWritingThaiDigits) {
        Locale.setDefault(localeWritingThaiDigits);

        String script = ShiftedClock.scriptShiftingTo(NEW_YEAR_2026);

        assertThat(script).contains("const shift = 1767225600000 - Date.now();");
    }
}
