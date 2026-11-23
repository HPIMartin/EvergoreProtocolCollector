package {{BASE_PACKAGE}}.acceptance.browser;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.bidi.module.Script;
import org.openqa.selenium.bidi.script.EvaluateResult;
import org.openqa.selenium.bidi.script.EvaluateResultSuccess;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

public class ShiftedClock {
    private static final Duration CLOCK_TOLERANCE = Duration.ofMinutes(1);
    private static final String SHIFTED_CLOCK = """
            () => {
              const shift = %d - Date.now();
              const RealDate = Date;
              const shiftedNow = () => RealDate.now() + shift;
              const ScenarioDate = new Proxy(RealDate, {
                construct(target, args, newTarget) { return Reflect.construct(target, args.length === 0 ? [shiftedNow()] : args, newTarget); },
                apply() { return new RealDate(shiftedNow()).toString(); },
                get(target, key, receiver) { return key === 'now' ? shiftedNow : Reflect.get(target, key, receiver); }
              });
              RealDate.prototype.constructor = ScenarioDate;
              globalThis.Date = ScenarioDate;
              const prototype = Intl.DateTimeFormat.prototype;
              const formatOf = Object.getOwnPropertyDescriptor(prototype, 'format').get;
              const formats = new WeakMap();
              Object.defineProperty(prototype, 'format', {
                configurable: true,
                get() {
                  if (!formats.has(this)) {
                    const format = formatOf.call(this);
                    formats.set(this, date => format(date === undefined ? shiftedNow() : date));
                  }
                  return formats.get(this);
                }
              });
              const formatToParts = prototype.formatToParts;
              Object.defineProperty(prototype, 'formatToParts', {
                configurable: true,
                writable: true,
                value(date) { return formatToParts.call(this, date === undefined ? shiftedNow() : date); }
              });
            }
            """;

    private final Supplier<WebDriver> openedDriver;
    private Instant clock;

    ShiftedClock(Supplier<WebDriver> openedDriver) {
        this.openedDriver = openedDriver;
    }

    public void reads(Instant instant) {
        clock = instant;
        if (openedDriver.get() != null) {
            shift();
        }
    }

    void shiftIfRead() {
        if (clock != null) {
            shift();
        }
    }

    void assertIsShown() {
        if (clock == null) {
            return;
        }
        EvaluateResult evaluated = script().evaluateFunctionInBrowsingContext(openedDriver.get().getWindowHandle(), "Date.now()", false, Optional.empty());
        long browserNow = numberIn(evaluated).orElseThrow(() -> new AssertionError("The browser answered no number for Date.now(): " + evaluated));

        assertThat(browserNow).as("the browser reads the scenario clock").isCloseTo(clock.toEpochMilli(), within(CLOCK_TOLERANCE.toMillis()));
    }

    static String scriptShiftingTo(Instant instant) {
        return String.format(Locale.ROOT, SHIFTED_CLOCK, instant.toEpochMilli());
    }

    private void shift() {
        script().addPreloadScript(scriptShiftingTo(clock));
    }

    private Script script() {
        return new Script(BiDiDriver.of(openedDriver.get()));
    }

    private static Optional<Long> numberIn(EvaluateResult result) {
        if (result instanceof EvaluateResultSuccess success && success.getResult().getValue().orElse(null) instanceof Number now) {
            return Optional.of(now.longValue());
        }
        return Optional.empty();
    }
}
