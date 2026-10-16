package dev.schoenberg.evergore.protocolParser.acceptance.browser;

import java.lang.reflect.Type;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.bidi.BiDi;
import org.openqa.selenium.bidi.Command;
import org.openqa.selenium.bidi.HasBiDi;
import org.openqa.selenium.json.TypeToken;
import org.openqa.selenium.remote.Augmenter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

public class ShiftedClock {
	private static final Duration CLOCK_TOLERANCE = Duration.ofMinutes(1);
	private static final String SHIFTED_CLOCK = "() => { const shift = %d - Date.now(); const RealDate = Date;"
			+ " class ScenarioDate extends RealDate { constructor(...parts) { if (parts.length === 0) { super(RealDate.now() + shift) } else { super(...parts) } }"
			+ " static now() { return RealDate.now() + shift } } globalThis.Date = ScenarioDate }";
	private static final Type BIDI_RESULT = new TypeToken<Map<String, Object>>() {}.getType();

	private final Supplier<WebDriver> openedDriver;
	private Instant clock;
	private String clockScript;

	ShiftedClock(Supplier<WebDriver> openedDriver) {
		this.openedDriver = openedDriver;
	}

	public void reads(Instant instant) {
		clock = instant;
		if (openedDriver.get() != null) {
			forget();
			shift();
		}
	}

	void shiftIfRead() {
		if (clock != null) {
			shift();
		}
	}

	void forget() {
		if (clockScript != null) {
			bidi().send(new Command<Map<String, Object>>("script.removePreloadScript", Map.of("script", clockScript), BIDI_RESULT));
			clockScript = null;
		}
	}

	void assertIsShown() {
		if (clock == null) {
			return;
		}
		Map<String, Object> evaluated = bidi()
				.send(new Command<Map<String, Object>>("script.evaluate",
						Map.of("expression", "Date.now()", "target", Map.of("context", openedDriver.get().getWindowHandle()), "awaitPromise", false), BIDI_RESULT));
		long browserNow = numberIn(evaluated.get("result")).orElseThrow(() -> new AssertionError("The browser answered no number for Date.now(): " + evaluated));

		assertThat(browserNow).as("the member's browser reads the scenario's clock").isCloseTo(clock.toEpochMilli(), within(CLOCK_TOLERANCE.toMillis()));
	}

	private void shift() {
		Map<String, Object> added = bidi()
				.send(new Command<Map<String, Object>>("script.addPreloadScript", Map.of("functionDeclaration", SHIFTED_CLOCK.formatted(clock.toEpochMilli())), BIDI_RESULT));
		clockScript = (String) added.get("script");
	}

	private BiDi bidi() {
		WebDriver driver = openedDriver.get();
		WebDriver augmented = driver instanceof HasBiDi ? driver : new Augmenter().augment(driver);
		return ((HasBiDi) augmented).getBiDi();
	}

	private static Optional<Long> numberIn(Object result) {
		return result instanceof Map<?, ?> value && value.get("value") instanceof Number now ? Optional.of(now.longValue()) : Optional.empty();
	}
}
