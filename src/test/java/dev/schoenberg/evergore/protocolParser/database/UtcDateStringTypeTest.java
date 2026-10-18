package dev.schoenberg.evergore.protocolParser.database;

import java.util.Date;
import java.util.Locale;
import java.util.Locale.Category;
import java.util.TimeZone;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UtcDateStringTypeTest {
	private static final UtcDateStringType TESTED = UtcDateStringType.getSingleton();
	private static final long SECOND_PASS_OF_TWO_THIRTY_AND_123_MS = 1792891800123L;

	private TimeZone zoneBefore;
	private Locale localeBefore;
	private Locale formatLocaleBefore;
	private Locale displayLocaleBefore;

	@BeforeEach
	void rememberTheJvmDefaults() {
		zoneBefore = TimeZone.getDefault();
		localeBefore = Locale.getDefault();
		formatLocaleBefore = Locale.getDefault(Category.FORMAT);
		displayLocaleBefore = Locale.getDefault(Category.DISPLAY);
	}

	@AfterEach
	void restoreTheJvmDefaults() {
		TimeZone.setDefault(zoneBefore);
		Locale.setDefault(localeBefore);
		Locale.setDefault(Category.FORMAT, formatLocaleBefore);
		Locale.setDefault(Category.DISPLAY, displayLocaleBefore);
	}

	@Test
	void writesTheTextInUtcWithItsMillisecondsAsSixDigitsInAZoneWithSummerTime() {
		TimeZone.setDefault(TimeZone.getTimeZone("Europe/Berlin"));

		Object text = TESTED.javaToSqlArg(null, new Date(SECOND_PASS_OF_TWO_THIRTY_AND_123_MS));

		assertThat(text).isEqualTo("2026-10-25 01:30:00.000123");
	}

	@Test
	void writesAsciiDigitsUnderALocaleWithDigitsOfItsOwn() {
		Locale.setDefault(Locale.forLanguageTag("fa-IR"));

		Object text = TESTED.javaToSqlArg(null, new Date(SECOND_PASS_OF_TWO_THIRTY_AND_123_MS));

		assertThat(text).isEqualTo("2026-10-25 01:30:00.000123");
	}

	@Test
	void readsTheSixDigitFractionAsMilliseconds() {
		Object read = TESTED.sqlArgToJava(null, "2026-10-25 01:30:00.000123", 0);

		assertThat(read).isEqualTo(new Date(SECOND_PASS_OF_TWO_THIRTY_AND_123_MS));
	}

	@Test
	void readsATextWithoutAFractionAsWholeSeconds() {
		Object read = TESTED.sqlArgToJava(null, "2026-10-25 01:30:00", 0);

		assertThat(read).isEqualTo(new Date(1792891800000L));
	}

	@Test
	void readsATextBeforeTheEpoch() {
		Object read = TESTED.sqlArgToJava(null, "1969-12-31 23:59:59.000999", 0);

		assertThat(read).isEqualTo(new Date(-1L));
	}
}
